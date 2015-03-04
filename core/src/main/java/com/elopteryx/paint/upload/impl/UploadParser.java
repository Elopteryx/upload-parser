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
package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.OnError;
import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.OnRequestComplete;
import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.UploadResponse;
import com.elopteryx.paint.upload.errors.PartSizeException;
import com.elopteryx.paint.upload.errors.RequestSizeException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.IntSupplier;

import static java.util.Objects.requireNonNull;

/**
 * Base class for the parser implementations. This holds the common methods, like the more specific
 * validation and the calling of the user-supplied functions.
 */
@SuppressWarnings("unchecked")
public abstract class UploadParser<T extends UploadParser<T>> implements MultipartParser.PartHandler {

    /**
     * The request object.
     */
    protected final HttpServletRequest request;

    /**
     * The response object.
     */
    protected UploadResponse uploadResponse;

    /**
     * The part begin callback, called at the beginning of each part parsing. Mandatory.
     */
    protected OnPartBegin partBeginCallback;

    /**
     * The part end callback, called at the end of each part parsing. Mandatory.
     */
    protected OnPartEnd partEndCallback;

    /**
     * The request callback, called after every part has been processed. Optional.
     */
    protected OnRequestComplete requestCallback;

    /**
     * The error callback, called when an error occurred. Optional.
     */
    protected OnError errorCallback;

    /**
     * The number of bytes that should be buffered before calling the part begin callback.
     */
    protected int sizeThreshold;

    /**
     * The maximum size permitted for the parts. By default it is unlimited.
     */
    protected long maxPartSize = -1;

    /**
     * The maximum size permitted for the complete request. By default it is unlimited.
     */
    protected long maxRequestSize = -1;

    /**
     * Protected constructor, to prevent invalid usages.
     * @param request The servlet request
     */
    public UploadParser(@Nonnull HttpServletRequest request) {
        this.request = requireNonNull(request);
    }

    /**
     * Sets a callback for each part, called at the beginning.
     * @param partBeginCallback An object or lambda expression
     * @return The parser will return itself
     */
    public T onPartBegin(@Nonnull OnPartBegin partBeginCallback) {
        this.partBeginCallback = requireNonNull(partBeginCallback);
        return (T) this;
    }

    /**
     * Sets a callback for each part, called at the end.
     * @param partEndCallback An object or lambda expression
     * @return The parser will return itself
     */
    public T onPartEnd(@Nonnull OnPartEnd partEndCallback) {
        this.partEndCallback = requireNonNull(partEndCallback);
        return (T) this;
    }

    /**
     * Sets a callback for the request, called after each part is processed.
     * @param requestCallback An object or lambda expression
     * @return The parser will return itself
     */
    public T onRequestComplete(@Nonnull OnRequestComplete requestCallback) {
        this.requestCallback = requireNonNull(requestCallback);
        return (T) this;
    }

    /**
     * Sets a callback for the errors, called if any error occurs.
     * @param errorCallback An object or lambda expression
     * @return The parser will return itself
     */
    public T onError(@Nonnull OnError errorCallback) {
        this.errorCallback = requireNonNull(errorCallback);
        return (T) this;
    }

    /**
     * Sets the servlet response object. This is only necessary to allow
     * access to it during the stages of the parsing. Note that if the
     * declaration of the custom functions are in the method which has
     * the response object as a parameter then this method can be skipped
     * and the parameter reference can be used instead.
     * @param uploadResponse The response wrapper
     * @return The parser will return itself
     */
    public T withResponse(@Nonnull UploadResponse uploadResponse) {
        this.uploadResponse = uploadResponse;
        return (T) this;
    }

    /**
     * Sets the amount of bytes to buffer in the memory, before
     * calling the part end callback.
     * @param sizeThreshold The amount to use
     * @return The parser will return itself
     */
    public T sizeThreshold(@Nonnegative int sizeThreshold) {
        this.sizeThreshold = Math.max(sizeThreshold, 0);
        return (T) this;
    }

    /**
     * Sets the amount of bytes to buffer in the memory, before
     * calling the part end callback.
     * @param supplier The supplier, which returns the amount to use
     * @return The parser will return itself
     */
    public T sizeThreshold(@Nonnegative @Nonnull IntSupplier supplier) {
        this.sizeThreshold = Math.max(supplier.getAsInt(), 0);
        return (T) this;
    }

    /**
     * Sets the maximum allowed size for each part. Exceeding this
     * will result in a {@link com.elopteryx.paint.upload.errors.PartSizeException} exception.
     * @param maxPartSize The amount to use
     * @return The parser will return itself
     */
    public T maxPartSize(@Nonnegative long maxPartSize) {
        this.maxPartSize = Math.max(maxPartSize, -1);
        return (T) this;
    }

    /**
     * Sets the maximum allowed size for each part. Exceeding this
     * will result in a {@link com.elopteryx.paint.upload.errors.PartSizeException} exception.
     * @param supplier The supplier, which returns the amount to use
     * @return The parser will return itself
     */
    public T maxPartSize(@Nonnegative @Nonnull IntSupplier supplier) {
        this.maxPartSize = Math.max(supplier.getAsInt(), -1);
        return (T) this;
    }

    /**
     * Sets the maximum allowed size for the request. Exceeding this
     * will result in a {@link com.elopteryx.paint.upload.errors.RequestSizeException} exception.
     * @param maxRequestSize The amount to use
     * @return The parser will return itself
     */
    public T maxRequestSize(@Nonnegative long maxRequestSize) {
        this.maxRequestSize = Math.max(maxRequestSize, -1);
        return (T) this;
    }

    /**
     * Sets the maximum allowed size for the request. Exceeding this
     * will result in a {@link com.elopteryx.paint.upload.errors.RequestSizeException} exception.
     * @param supplier The supplier, which returns the amount to use
     * @return The parser will return itself
     */
    public T maxRequestSize(@Nonnegative @Nonnull IntSupplier supplier) {
        this.maxRequestSize = Math.max(supplier.getAsInt(), -1);
        return (T) this;
    }

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private static final int BUFFER_SIZE = 1024;

    private ByteBuffer checkBuffer;

    private WritableByteChannel writableChannel;

    private long requestSize;

    protected ServletInputStream servletInputStream;

    protected UploadContextImpl context;

    protected MultipartParser.ParseState parseState;

    protected final byte[] buf = new byte[BUFFER_SIZE];

    /**
     * Sets up the necessary objects to start the parsing. Depending upon
     * the environment the concrete implementations can be very different.
     * @throws IOException If an error occurs with the IO
     */
    protected void init() throws IOException {
        requireNonNull(partBeginCallback, "Setting a valid part begin callback is mandatory!");
        requireNonNull(partEndCallback, "Setting a valid part end callback is mandatory!");

        //Fail fast mode
        if (maxRequestSize > -1) {
            long requestSize = request.getContentLengthLong();
            if (requestSize > maxRequestSize)
                throw new RequestSizeException("The size of the request (" + requestSize +
                        ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
        }

        checkBuffer = ByteBuffer.allocate(sizeThreshold);
        context = new UploadContextImpl(request, uploadResponse);

        String mimeType = request.getHeader(PartStreamHeaders.CONTENT_TYPE);
        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = PartStreamHeaders.extractBoundaryFromHeader(mimeType);
            if (boundary == null) {
                throw new RuntimeException("Could not find boundary in multipart request with ContentType: "+mimeType+", multipart data will not be available");
            }
            Charset charset = request.getCharacterEncoding() != null ? Charset.forName(request.getCharacterEncoding()) : StandardCharsets.ISO_8859_1;
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), charset);
        }

        servletInputStream = request.getInputStream();
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     * @param additional The amount to add, always non negative
     */
    protected void checkPartSize(int additional) {
        long partSize = context.incrementAndGetPartBytesRead(additional);
        if (maxPartSize > -1 && partSize > maxPartSize)
            throw new PartSizeException("The size of the part (" + partSize +
                    ") is greater than the allowed size (" + maxPartSize + ")!", partSize, maxPartSize);
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     * @param additional The amount to add, always non negative
     */
    protected void checkRequestSize(int additional) {
        requestSize += additional;
        if (maxRequestSize > -1 && requestSize > maxRequestSize)
            throw new RequestSizeException("The size of the request (" + requestSize +
                    ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
    }

    @Override
    public void beginPart(final PartStreamHeaders headers) {
        final String disposition = headers.getHeader(PartStreamHeaders.CONTENT_DISPOSITION);
        if (disposition != null && disposition.startsWith("form-data")) {
            String fieldName = PartStreamHeaders.extractQuotedValueFromHeader(disposition, "name");
            String fileName = PartStreamHeaders.extractQuotedValueFromHeader(disposition, "filename");
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
        if(!context.isBuffering()) {
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
        PartOutput output = requireNonNull(partBeginCallback.onPartBegin(context, checkBuffer));
        if(output.safeToCast(WritableByteChannel.class))
            writableChannel = output.get(WritableByteChannel.class);
        else if(output.safeToCast(OutputStream.class))
            writableChannel = Channels.newChannel(output.get(OutputStream.class));
        else
            throw new IllegalArgumentException("Invalid output object!");
        context.setOutput(output);
        context.finishBuffering();
        checkBuffer.flip();
        while (checkBuffer.hasRemaining()) {
            writableChannel.write(checkBuffer);
        }
    }

    @Override
    public void endPart() throws IOException {
        if(context.isBuffering()) {
            validate();
        }
        checkBuffer.clear();
        context.updatePartBytesRead();
        partEndCallback.onPartEnd(context);
    }
}
