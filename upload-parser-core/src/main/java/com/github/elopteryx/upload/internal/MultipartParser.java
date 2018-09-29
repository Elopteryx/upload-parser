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

package com.github.elopteryx.upload.internal;

import com.github.elopteryx.upload.errors.MultipartException;

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
 * handler during certain stages.</p>
 *
 * @author Stuart Douglas
 */
public final class MultipartParser {

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
    
    private static final String ERROR_MESSAGE = "Invalid multipart request!";

    private MultipartParser() {
        // No need to instantiate
    }

    interface PartHandler {
        void beginPart(final Headers headers);

        void data(final ByteBuffer buffer) throws IOException;

        void endPart() throws IOException;
    }

    /**
     * Begins parsing the multipart input, sets up the necessary objects.
     * @param handler The part handler, which is to be called at certain points.
     * @param boundary The boundary value for the multipart stream.
     * @param bufferSize The size of the buffer for the encoding handlers.
     * @param requestCharset The charset of the input.
     * @return A new state object to allow calling the parser.
     */
    public static ParseState beginParse(final PartHandler handler, final byte[] boundary, final int bufferSize, final Charset requestCharset) {

        // We prepend CR/LF to the boundary to chop trailing CR/LF from body-data tokens.
        final var boundaryToken = new byte[boundary.length + BOUNDARY_PREFIX.length];
        System.arraycopy(BOUNDARY_PREFIX, 0, boundaryToken, 0, BOUNDARY_PREFIX.length);
        System.arraycopy(boundary, 0, boundaryToken, BOUNDARY_PREFIX.length, boundary.length);
        return new ParseState(handler, bufferSize, requestCharset, boundaryToken);
    }

    static class ParseState {
        private final PartHandler partHandler;
        private final Charset requestCharset;
        private final int bufferSize;

        /**
         * The boundary, complete with the initial CRLF--.
         */
        private final byte[] boundary;

        // 0=preamble
        private int state;
        private int subState = Integer.MAX_VALUE; // used for preamble parsing
        private ByteArrayOutputStream currentString;
        private String currentHeaderName;
        private Headers headers;
        private Encoding encodingHandler;

        /**
         * Public constructor.
         * @param partHandler The part handler, which is to be called at certain points.
         * @param requestCharset The charset of the input.
         * @param bufferSize The size of the allocated buffer.
         * @param boundary The boundary value for the multipart stream.
         */
        ParseState(final PartHandler partHandler, final int bufferSize, final Charset requestCharset, final byte[] boundary) {
            this.partHandler = partHandler;
            this.requestCharset = requestCharset;
            this.bufferSize = bufferSize;
            this.boundary = boundary;
        }

        /**
         * Parses the given data. This method can be called by the blocking and async upload parser as well.
         * @param buffer The buffer containing new data to process
         * @throws IOException If an error occurred with the I/O
         */
        void parse(final ByteBuffer buffer) throws IOException {
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
                        throw new IllegalStateException(String.valueOf(state));
                }
            }
        }

        private void preamble(final ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                final var b = buffer.get();
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
                        headers = new Headers();
                        return;
                    } else {
                        subState = -1;
                    }
                }
            }
        }

        private void headerName(final ByteBuffer buffer) throws MultipartException {
            while (buffer.hasRemaining()) {
                final var b = buffer.get();
                if (b == ':') {
                    if (currentString == null || subState != 0) {
                        throw new MultipartException(ERROR_MESSAGE);
                    } else {
                        currentHeaderName = new String(currentString.toByteArray(), requestCharset);
                        currentString.reset();
                        subState = 0;
                        state = 2;
                        return;
                    }
                } else if (b == CR) {
                    if (currentString == null) {
                        subState = 1;
                    } else {
                        throw new MultipartException(ERROR_MESSAGE);
                    }
                } else if (b == LF) {
                    if (currentString != null || subState != 1) {
                        throw new MultipartException(ERROR_MESSAGE);
                    }
                    state = 3;
                    subState = 0;
                    partHandler.beginPart(headers);
                    //select the appropriate encoding
                    final var encoding = headers.getHeader(CONTENT_TRANSFER_ENCODING);
                    if (encoding == null) {
                        encodingHandler = new IdentityEncoding();
                    } else if (encoding.equalsIgnoreCase("base64")) {
                        encodingHandler = new Base64Encoding(bufferSize);
                    } else if (encoding.equalsIgnoreCase("quoted-printable")) {
                        encodingHandler = new QuotedPrintableEncoding(bufferSize);
                    } else {
                        encodingHandler = new IdentityEncoding();
                    }
                    headers = null;
                    return;

                } else {
                    if (subState != 0) {
                        throw new MultipartException(ERROR_MESSAGE);
                    } else if (currentString == null) {
                        currentString = new ByteArrayOutputStream();
                    }
                    currentString.write(b);
                }
            }
        }

        private void headerValue(final ByteBuffer buffer) throws MultipartException {
            while (buffer.hasRemaining()) {
                final var b = buffer.get();
                if (b == CR) {
                    subState = 1;
                } else if (b == LF) {
                    if (subState != 1) {
                        throw new MultipartException(ERROR_MESSAGE);
                    }
                    headers.addHeader(currentHeaderName.trim(), new String(currentString.toByteArray(), requestCharset).trim());
                    state = 1;
                    subState = 0;
                    currentString = null;
                    return;
                } else {
                    if (subState != 0) {
                        throw new MultipartException(ERROR_MESSAGE);
                    }
                    currentString.write(b);
                }
            }
        }

        private void entity(final ByteBuffer buffer) throws IOException {
            var startingSubState = subState;
            final var pos = buffer.position();
            while (buffer.hasRemaining()) {
                final var b = buffer.get();
                if (subState >= 0) {
                    if (b == boundary[subState]) {
                        //if we have a potential boundary match
                        subState++;
                        if (subState == boundary.length) {
                            startingSubState = 0;
                            //we have our data
                            final var retBuffer = buffer.duplicate();
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
                        headers = new Headers();
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
            final var retBuffer = buffer.duplicate();
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

        boolean isComplete() {
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

        private final ByteBuffer buffer;

        Base64Encoding(final int size) {
            buffer = ByteBuffer.allocate(size);
        }

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

        private final ByteBuffer buffer;

        QuotedPrintableEncoding(final int size) {
            buffer = ByteBuffer.allocate(size);
        }

        @Override
        public void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException {
            var equalsSeen = this.equalsSeen;
            var firstCharacter = this.firstCharacter;
            buffer.clear();
            try {
                while (rawData.hasRemaining()) {
                    final var readByte = rawData.get();
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
                            var result = Character.digit((char) firstCharacter, 16);
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
