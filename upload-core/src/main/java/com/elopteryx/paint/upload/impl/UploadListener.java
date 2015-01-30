/*
 * Copyright (C) 2015- Adam Forgacs
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

import com.elopteryx.paint.upload.UploadParser;
import com.elopteryx.paint.upload.errors.MalformedMessageException;
import com.elopteryx.paint.upload.errors.PartSizeException;
import com.elopteryx.paint.upload.errors.RequestSizeException;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Asynchronous upload listener. Called by the servlet container whenever data is available.
 * This class is only public to serve as an entry point in the implementation package, users
 * should not need to directly depend on this class.
 */
public class UploadListener extends UploadParser implements ReadListener, MultipartParser.PartHandler {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private static final int BUFFER_SIZE = 1024;
    
    private ByteBuffer checkBuffer;

    private ServletInputStream servletInputStream;

    private UploadContextImpl context;

    private WritableByteChannel writableChannel;
    
    private MultipartParser.ParseState parseState;

    private static final Charset defaultEncoding = StandardCharsets.ISO_8859_1;

    private final byte[] buf = new byte[BUFFER_SIZE];

    private long requestSize;

    public UploadListener(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public void setup() throws IOException {
        Objects.requireNonNull(partValidator, "Setting a valid part validator is mandatory!");
        Objects.requireNonNull(partExecutor, "Setting a valid part executor is mandatory!");

        //Fail fast mode
        if (maxRequestSize > -1) {
            long requestSize = request.getContentLengthLong();
            if (requestSize > maxRequestSize)
                throw new RequestSizeException("The size of the request (" + requestSize +
                        ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
        }

        checkBuffer = ByteBuffer.allocate(sizeThreshold);
        context = new UploadContextImpl(request, response);

        String mimeType = request.getHeader(PartStreamHeaders.CONTENT_TYPE);
        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = PartStreamHeaders.extractTokenFromHeader(mimeType, "boundary");
            if (boundary == null) {
                throw new RuntimeException("Could not find boundary in multipart request with ContentType: "+mimeType+", multipart data will not be available");
            }
            Charset charset = request.getCharacterEncoding() != null ? Charset.forName(request.getCharacterEncoding()) : defaultEncoding;
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), charset);
        }

        servletInputStream = request.getInputStream();
        if (!request.isAsyncStarted())
            request.startAsync();
        servletInputStream.setReadListener(this);
    }

    /**
     * When an instance of the ReadListener is registered with a ServletInputStream, this method will be invoked
     * by the container the first time when it is possible to read data. Subsequently the container will invoke
     * this method if and only if ServletInputStream.isReady() method has been called and has returned false.
     * @throws IOException if an I/O related error has occurred during processing
     */
    @Override
    public void onDataAvailable() throws IOException {
        while (servletInputStream.isReady()) {
            parseCurrentItem();
        }
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     */
    private void checkPartSize(int additional) {
        long partSize = context.incrementAndGetPartBytesRead(additional);
        if (maxPartSize > -1 && partSize > maxPartSize)
            throw new PartSizeException("The size of the part (" + partSize +
                    ") is greater than the allowed size (" + maxPartSize + ")!", partSize, maxPartSize);
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     */
    private void checkRequestSize(int additional) {
        requestSize += additional;
        if (maxRequestSize > -1 && requestSize > maxRequestSize)
            throw new RequestSizeException("The size of the request (" + requestSize +
                    ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
    }
    
    /**
     * Parses the servlet stream once. Will switch to a new item
     * if the current one is fully read.
     *
     * @return Whether it should be called again
     * @throws java.io.IOException
     */
    private boolean parseCurrentItem() throws IOException {
        int c = servletInputStream.read(buf);
        if (c == -1) {
            if (!parseState.isComplete())
                throw new MalformedMessageException();
        } else {
            checkRequestSize(c);
            parseState.parse(ByteBuffer.wrap(buf, 0, c));
        }
        return !parseState.isComplete();
    }

    @Override
    public void beginPart(final PartStreamHeaders headers) {
        final String disposition = headers.getHeader(PartStreamHeaders.CONTENT_DISPOSITION);
        if (disposition != null) {
            if (disposition.startsWith("form-data")) {
                String fieldName = PartStreamHeaders.extractQuotedValueFromHeader(disposition, "name");
                String fileName = PartStreamHeaders.extractQuotedValueFromHeader(disposition, "filename");
                context.reset(new PartStreamImpl(fileName, fieldName, headers.getHeader(PartStreamHeaders.CONTENT_TYPE), headers));
            }
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
        writableChannel = Objects.requireNonNull(partValidator.apply(context, checkBuffer));
        context.setChannel(writableChannel);
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
        partExecutor.accept(context);
    }

    /**
     * Invoked when all data for the current request has been read.
     * @throws IOException if an I/O related error has occurred during processing
     */
    @Override
    public void onAllDataRead() throws IOException {
        // After the servlet input stream is finished there are still unread bytes or
        // in case of fast uploads or small sizes the initial parse can read the whole
        // input stream, causing the {@link #onDataAvailable} not to be called even once.
        while (true) {
            if (!parseCurrentItem())
                break;
        }
        try {
            if(completeExecutor != null)
                completeExecutor.accept(context);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        request.getAsyncContext().complete();
    }

    /**
     * Invoked when an error occurs processing the request.
     * @param t The unhandled error that happened
     */
    @Override
    public void onError(Throwable t) {
        if(errorExecutor != null)
            errorExecutor.accept(context, t);
    }
}
