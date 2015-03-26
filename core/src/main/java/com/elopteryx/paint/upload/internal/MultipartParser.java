/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.elopteryx.paint.upload.internal;

import com.elopteryx.paint.upload.errors.MultipartException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Copied from Undertow. Made some refactoring to remove the
 * dependency on XNio. Instead of a pool the methods receive
 * a ByteBuffer instance to use.
 *
 * <p>The parser which reads the multipart stream and calls the part
 * handler during certain stages.
 *
 * @author Stuart Douglas
 */
public class MultipartParser {

    /**
     * The default size for the buffers. 
     */
    private static final int BUFFER_SIZE = 1024;
    /**
     * The Carriage Return ASCII character value.
     */
    private static final byte CR = 0x0D;
    /**
     * The Line Feed ASCII character value.
     */
    private static final byte LF = 0x0A;
    /**
     * The dash (-) ASCII character value.
     */
    private static final byte DASH = 0x2D;
    /**
     * A byte sequence that precedes a boundary (<code>CRLF--</code>).
     */
    private static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};

    private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

    public interface PartHandler {
        void beginPart(final PartStreamHeaders headers);

        void data(final ByteBuffer buffer) throws IOException;

        void endPart() throws IOException;
    }

    /**
     * Begins parsing the multipart input, sets up the necessary objects.
     * @param handler The part handler, which is to be called at certain points.
     * @param boundary The boundary value for the multipart stream.
     * @param requestCharset The charset of the input.
     * @return A new state object to allow calling the parser.
     */
    public static ParseState beginParse(final PartHandler handler, final byte[] boundary, final Charset requestCharset) {

        // We prepend CR/LF to the boundary to chop trailing CR/LF from body-data tokens.
        byte[] boundaryToken = new byte[boundary.length + BOUNDARY_PREFIX.length];
        System.arraycopy(BOUNDARY_PREFIX, 0, boundaryToken, 0, BOUNDARY_PREFIX.length);
        System.arraycopy(boundary, 0, boundaryToken, BOUNDARY_PREFIX.length, boundary.length);
        return new ParseState(handler, requestCharset, boundaryToken);
    }

    public static class ParseState {
        private final PartHandler partHandler;
        private final Charset requestCharset;

        /**
         * The boundary, complete with the initial CRLF--.
         */
        private final byte[] boundary;

        // 0=preamble
        private volatile int state = 0;
        private volatile int subState = Integer.MAX_VALUE; // used for preamble parsing
        private volatile ByteArrayOutputStream currentString = null;
        private volatile String currentHeaderName = null;
        private volatile PartStreamHeaders headers;
        private volatile Encoding encodingHandler;

        /**
         * Public constructor.
         * @param partHandler The part handler, which is to be called at certain points.
         * @param requestCharset The charset of the input.
         * @param boundary The boundary value for the multipart stream.
         */
        public ParseState(final PartHandler partHandler, Charset requestCharset, final byte[] boundary) {
            this.partHandler = partHandler;
            this.requestCharset = requestCharset;
            this.boundary = boundary;
        }

        /**
         * Parses the given data. This method can be called by the blocking and async upload parser as well.
         * @param buffer The buffer containing new data to process
         * @throws IOException If an error occurred with the I/O
         */
        public void parse(ByteBuffer buffer) throws IOException {
            while (buffer.hasRemaining()) {
                switch (state) {
                    case 0:
                        preamble(buffer);
                        break;
                    case 1:
                        headerName(buffer);
                        break;
                    case 2:
                        headerValue(buffer);
                        break;
                    case 3:
                        entity(buffer);
                        break;
                    case -1:
                        return;
                    default:
                        throw new IllegalStateException("" + state);
                }
            }
        }

        private void preamble(final ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                final byte b = buffer.get();
                if (subState >= 0) {
                    //handle the case of no preamble. In this case there is no CRLF
                    if (subState == Integer.MAX_VALUE) {
                        subState = boundary[2] == b ? 2 : 0;
                    }
                    if (b == boundary[subState]) {
                        subState++;
                        if (subState == boundary.length) {
                            subState = -1;
                        }
                    } else if (b == boundary[0]) {
                        subState = 1;
                    } else {
                        subState = 0;
                    }
                } else if (subState == -1) {
                    if (b == CR) {
                        subState = -2;
                    }
                } else if (subState == -2) {
                    if (b == LF) {
                        subState = 0;
                        state = 1;//preamble is done
                        headers = new PartStreamHeaders();
                        return;
                    } else {
                        subState = -1;
                    }
                }
            }
        }

        private void headerName(final ByteBuffer buffer) throws MultipartException {
            while (buffer.hasRemaining()) {
                final byte b = buffer.get();
                if (b == ':') {
                    if (currentString == null || subState != 0) {
                        throw new MultipartException();
                    } else {
                        currentHeaderName = new String(currentString.toByteArray(), requestCharset);
                        currentString.reset();
                        subState = 0;
                        state = 2;
                        return;
                    }
                } else if (b == CR) {
                    if (currentString != null) {
                        throw new MultipartException();
                    } else {
                        subState = 1;
                    }
                } else if (b == LF) {
                    if (currentString != null || subState != 1) {
                        throw new MultipartException();
                    }
                    state = 3;
                    subState = 0;
                    partHandler.beginPart(headers);
                    //select the appropriate encoding
                    String encoding = headers.getHeader(CONTENT_TRANSFER_ENCODING);
                    if (encoding == null) {
                        encodingHandler = new IdentityEncoding();
                    } else if (encoding.equalsIgnoreCase("base64")) {
                        encodingHandler = new Base64Encoding();
                    } else if (encoding.equalsIgnoreCase("quoted-printable")) {
                        encodingHandler = new QuotedPrintableEncoding();
                    } else {
                        encodingHandler = new IdentityEncoding();
                    }
                    headers = null;
                    return;

                } else {
                    if (subState != 0) {
                        throw new MultipartException();
                    } else if (currentString == null) {
                        currentString = new ByteArrayOutputStream();
                    }
                    currentString.write(b);
                }
            }
        }

        private void headerValue(final ByteBuffer buffer) throws MultipartException {
            while (buffer.hasRemaining()) {
                final byte b = buffer.get();
                if (b == CR) {
                    subState = 1;
                } else if (b == LF) {
                    if (subState != 1) {
                        throw new MultipartException();
                    }
                    headers.addHeader(currentHeaderName.trim(), new String(currentString.toByteArray(), requestCharset).trim());
                    state = 1;
                    subState = 0;
                    currentString = null;
                    return;
                } else {
                    if (subState != 0) {
                        throw new MultipartException();
                    }
                    currentString.write(b);
                }
            }
        }

        private void entity(final ByteBuffer buffer) throws IOException {
            int startingSubState = subState;
            int pos = buffer.position();
            while (buffer.hasRemaining()) {
                final byte b = buffer.get();
                if (subState >= 0) {
                    if (b == boundary[subState]) {
                        //if we have a potential boundary match
                        subState++;
                        if (subState == boundary.length) {
                            startingSubState = 0;
                            //we have our data
                            ByteBuffer retBuffer = buffer.duplicate();
                            retBuffer.position(pos);

                            retBuffer.limit(Math.max(buffer.position() - boundary.length, 0));
                            encodingHandler.handle(partHandler, retBuffer);
                            partHandler.endPart();
                            subState = -1;
                        }
                    } else if (b == boundary[0]) {
                        //we started half way through a boundary, but it turns out we did not actually meet the boundary condition
                        //so we call the part handler with our copy of the boundary data
                        if (startingSubState > 0) {
                            encodingHandler.handle(partHandler, ByteBuffer.wrap(boundary, 0, startingSubState));
                            startingSubState = 0;
                        }
                        subState = 1;
                    } else {
                        //we started half way through a boundary, but it turns out we did not actually meet the boundary condition
                        //so we call the part handler with our copy of the boundary data
                        if (startingSubState > 0) {
                            encodingHandler.handle(partHandler, ByteBuffer.wrap(boundary, 0, startingSubState));
                            startingSubState = 0;
                        }
                        subState = 0;
                    }
                } else if (subState == -1) {
                    if (b == CR) {
                        subState = -2;
                    } else if (b == DASH) {
                        subState = -3;
                    }
                } else if (subState == -2) {
                    if (b == LF) {
                        //ok, we have our data
                        subState = 0;
                        state = 1;
                        headers = new PartStreamHeaders();
                        return;
                    } else if (b == DASH) {
                        subState = -3;
                    } else {
                        subState = -1;
                    }
                } else if (subState == -3) {
                    if (b == DASH) {
                        state = -1; //we are done
                        return;
                    } else {
                        subState = -1;
                    }
                }
            }
            //handle the data we read so far
            ByteBuffer retBuffer = buffer.duplicate();
            retBuffer.position(pos);
            if (subState == 0) {
                //if we end partially through a boundary we do not handle the data
                encodingHandler.handle(partHandler, retBuffer);
            } else if (retBuffer.remaining() > subState && subState > 0) {
                //we have some data to handle, and the end of the buffer might be a boundary match
                retBuffer.limit(retBuffer.limit() - subState);
                encodingHandler.handle(partHandler, retBuffer);
            }
        }

        public boolean isComplete() {
            return state == -1;
        }
    }


    interface Encoding {
        void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException;
    }

    static class IdentityEncoding implements Encoding {

        @Override
        public void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException {
            handler.data(rawData);
            rawData.clear();
        }
    }

    static class Base64Encoding implements Encoding {

        private final Base64Decoder decoder = new Base64Decoder();

        private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        @Override
        public void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException {
            try {
                do {
                    buffer.clear();
                    decoder.decode(rawData, buffer);
                    buffer.flip();
                    handler.data(buffer);
                } while (rawData.hasRemaining());
            } finally {
                buffer.clear();
            }
        }
    }

    static class QuotedPrintableEncoding implements Encoding {

        boolean equalsSeen;
        byte firstCharacter;

        private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        @Override
        public void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException {
            boolean equalsSeen = this.equalsSeen;
            byte firstCharacter = this.firstCharacter;
            buffer.clear();
            try {
                while (rawData.hasRemaining()) {
                    byte readByte = rawData.get();
                    if (equalsSeen) {
                        if (firstCharacter == 0) {
                            if (readByte == '\n' || readByte == '\r') {
                                //soft line break
                                //ignore
                                equalsSeen = false;
                            } else {
                                firstCharacter = readByte;
                            }
                        } else {
                            int result = Character.digit((char) firstCharacter, 16);
                            result <<= 4; //shift it 4 bytes and then add the next value to the end
                            result += Character.digit((char) readByte, 16);
                            buffer.put((byte) result);
                            equalsSeen = false;
                            firstCharacter = 0;
                        }
                    } else if (readByte == '=') {
                        equalsSeen = true;
                    } else {
                        buffer.put(readByte);
                        if (!buffer.hasRemaining()) {
                            buffer.flip();
                            handler.data(buffer);
                            buffer.clear();
                        }
                    }
                }
                buffer.flip();
                handler.data(buffer);
            } finally {
                buffer.clear();
                this.equalsSeen = equalsSeen;
                this.firstCharacter = firstCharacter;
            }
        }
    }
}
