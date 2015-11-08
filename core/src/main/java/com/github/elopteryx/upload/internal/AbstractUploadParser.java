/*
 * Copyright (C) 2015 Adam Forgacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.elopteryx.upload.internal;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;

import com.github.elopteryx.upload.OnError;
import com.github.elopteryx.upload.OnPartBegin;
import com.github.elopteryx.upload.OnPartEnd;
import com.github.elopteryx.upload.OnRequestComplete;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.errors.PartSizeException;
import com.github.elopteryx.upload.errors.RequestSizeException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Base class for the parser implementations. This holds the common methods, like the more specific
 * validation and the calling of the user-supplied functions.
 */
public abstract class AbstractUploadParser implements MultipartParser.PartHandler {

    /**
     * The default size allocated for the buffers.
     */
    private static final int DEFAULT_USED_MEMORY = 4096;
    /**
     * The part begin callback, called at the beginning of each part parsing.
     */
    private OnPartBegin partBeginCallback;
    /**
     * The part end callback, called at the end of each part parsing.
     */
    private OnPartEnd partEndCallback;
    /**
     * The request callback, called after every part has been processed.
     */
    OnRequestComplete requestCallback;
    /**
     * The error callback, called when an error occurred.
     */
    OnError errorCallback;
    /**
     * The user object.
     */
    Object userObject;
    /**
     * The number of bytes to be allocated for the buffers.
     */
    protected int maxBytesUsed = DEFAULT_USED_MEMORY;
    /**
     * The number of bytes that should be buffered before calling the part begin callback.
     */
    protected int sizeThreshold;
    /**
     * The maximum size permitted for the parts. By default it is unlimited.
     */
    private long maxPartSize = -1;
    /**
     * The maximum size permitted for the complete request. By default it is unlimited.
     */
    protected long maxRequestSize = -1;
    /**
     * The valid mime type.
     */
    protected static final String MULTIPART_FORM_DATA = "multipart/form-data";
    /**
     * The buffer that stores the first bytes of the current part.
     */
    protected ByteBuffer checkBuffer;
    /**
     * The channel to where the current part is written.
     */
    private WritableByteChannel writableChannel;
    /**
     * The known size of the request.
     */
    protected long requestSize;
    /**
     * The context instance.
     */
    protected UploadContextImpl context;
    /**
     * The reference to the multipart parser.
     */
    protected MultipartParser.ParseState parseState;
    /**
     * The buffer that stores the bytes which were read from the
     * servlet input stream or from a different source.
     */
    protected byte[] buf;

    /**
     * Sets up the necessary objects to start the parsing. Depending upon
     * the environment the concrete implementations can be very different.
     * @param request The servlet request
     * @throws IOException If an error occurs with the IO
     */
    protected void init(HttpServletRequest request) throws IOException {

        // Fail fast mode
        if (maxRequestSize > -1) {
            long requestSize = request.getContentLengthLong();
            if (requestSize > maxRequestSize) {
                throw new RequestSizeException("The size of the request (" + requestSize
                        + ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
            }
        }

        checkBuffer = ByteBuffer.allocate(sizeThreshold);
        context = new UploadContextImpl(request, userObject);

        String mimeType = request.getHeader(Headers.CONTENT_TYPE);
        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = Headers.extractBoundaryFromHeader(mimeType);
            if (boundary == null) {
                throw new IllegalArgumentException("Could not find boundary in multipart request with ContentType: "
                        + mimeType
                        + ", multipart data will not be available");
            }
            String encodingHeader = request.getCharacterEncoding();
            Charset charset = encodingHeader != null ? Charset.forName(encodingHeader) : ISO_8859_1;
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), maxBytesUsed, charset);
        }
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     * @param additional The amount to add, always non negative
     */
    void checkPartSize(int additional) {
        long partSize = context.incrementAndGetPartBytesRead(additional);
        if (maxPartSize > -1 && partSize > maxPartSize) {
            throw new PartSizeException("The size of the part ("
                    + partSize
                    + ") is greater than the allowed size ("
                    + maxPartSize
                    + ")!", partSize, maxPartSize);
        }
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     * @param additional The amount to add, always non negative
     */
    protected void checkRequestSize(int additional) {
        requestSize += additional;
        if (maxRequestSize > -1 && requestSize > maxRequestSize) {
            throw new RequestSizeException("The size of the request ("
                    + requestSize
                    + ") is greater than the allowed size ("
                    + maxRequestSize
                    + ")!", requestSize, maxRequestSize);
        }
    }

    @Override
    public void beginPart(final Headers headers) {
        final String disposition = headers.getHeader(Headers.CONTENT_DISPOSITION);
        if (disposition != null && disposition.startsWith("form-data")) {
            String fieldName = Headers.extractQuotedValueFromHeader(disposition, "name");
            String fileName = Headers.extractQuotedValueFromHeader(disposition, "filename");
            context.reset(new PartStreamImpl(fileName, fieldName, headers));
        }
    }

    @Override
    public void data(final ByteBuffer buffer) throws IOException {
        checkPartSize(buffer.remaining());
        copyBuffer(buffer);
        if (context.isBuffering() && (context.getPartBytesRead() >= sizeThreshold)) {
            validate();
        }
        if (!context.isBuffering()) {
            while (buffer.hasRemaining()) {
                writableChannel.write(buffer);
            }
        }
    }

    private void copyBuffer(final ByteBuffer buffer) {
        int transferCount = Math.min(checkBuffer.remaining(), buffer.remaining());
        if (transferCount > 0) {
            checkBuffer.put(buffer.array(), buffer.arrayOffset() + buffer.position(), transferCount);
            buffer.position(buffer.position() + transferCount);
        }
    }

    private void validate() throws IOException {
        context.finishBuffering();
        PartOutput output = null;
        if (partBeginCallback != null) {
            output = requireNonNull(partBeginCallback.onPartBegin(context, checkBuffer));
            if (output.safeToCast(WritableByteChannel.class)) {
                writableChannel = output.unwrap(WritableByteChannel.class);
            } else if (output.safeToCast(OutputStream.class)) {
                writableChannel = Channels.newChannel(output.unwrap(OutputStream.class));
            } else if (output.safeToCast(Path.class)) {
                writableChannel = Files.newByteChannel(output.unwrap(Path.class), EnumSet.of(APPEND, CREATE, WRITE));
            } else {
                throw new IllegalArgumentException("Invalid output object!");
            }
        }
        if (output == null) {
            writableChannel = new NullChannel();
            output = PartOutput.from(writableChannel);
        }
        context.setOutput(output);
        checkBuffer.flip();
        while (checkBuffer.hasRemaining()) {
            writableChannel.write(checkBuffer);
        }
    }

    @Override
    public void endPart() throws IOException {
        if (context.isBuffering()) {
            validate();
        }
        checkBuffer.clear();
        context.updatePartBytesRead();
        writableChannel.close();
        if (partEndCallback != null) {
            partEndCallback.onPartEnd(context);
        }
    }

    public void setPartBeginCallback(OnPartBegin partBeginCallback) {
        this.partBeginCallback = partBeginCallback;
    }

    public void setPartEndCallback(OnPartEnd partEndCallback) {
        this.partEndCallback = partEndCallback;
    }

    public void setRequestCallback(OnRequestComplete requestCallback) {
        this.requestCallback = requestCallback;
    }

    public void setErrorCallback(OnError errorCallback) {
        this.errorCallback = errorCallback;
    }

    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    /**
     * Sets the amount of bytes to allocate. This is distributed
     * between the buffers used for raw parsing.
     * @param maxBytesUsed The amount to use
     */
    public void setMaxBytesUsed(int maxBytesUsed) {
        // There are two byte buffers so each one gets half of the amount
        this.maxBytesUsed = maxBytesUsed / 2;
        this.buf = new byte[maxBytesUsed / 2];
    }

    public void setSizeThreshold(int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public void setMaxPartSize(long maxPartSize) {
        this.maxPartSize = maxPartSize;
    }

    public void setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }
}
