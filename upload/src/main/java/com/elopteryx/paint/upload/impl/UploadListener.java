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
 * Asynchronous upload listener. Called by the servlet
 * container whenever data is available.
 */
public class UploadListener extends UploadParser implements ReadListener, MultipartParser.PartHandler {

    /**
     * Minimal buffer size.
     */
    private static final int MIN_BUFFER_SIZE = 8192;

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    
    private ByteBuffer buffer;

    private ServletInputStream servletInputStream;

    private UploadContextImpl context;

    private WritableByteChannel writableChannel;
    
    private MultipartParser.ParseState parseState;

    private static final Charset defaultEncoding = StandardCharsets.ISO_8859_1;

    private byte[] buf = new byte[8192];

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

        buffer = ByteBuffer.allocate(Math.max(MIN_BUFFER_SIZE, sizeThreshold));
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
        long partSize = context.incrementAndGetPartBytes(additional);
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
        //TODO copy the buffered bytes
        if(context.isBuffering()) {
        //if (context.isBuffering() && (context.getPartBytesRead() >= sizeThreshold || read == -1)) {
            writableChannel = Objects.requireNonNull(partValidator.apply(context, buffer.asReadOnlyBuffer()));
            context.finishBuffering();
        }
        if(!context.isBuffering()) {
            while (buffer.hasRemaining()) {
                writableChannel.write(buffer);
            }
        }
        
    }

    @Override
    public void endPart() {
        context.updatePartBytesRead();
        partExecutor.accept(context, writableChannel);
    }

    @Override
    public void onAllDataRead() throws IOException {
        // After the servlet input stream is finished there are still unread bytes or
        // in case of fast uploads or small sizes the initial parse can read the whole
        // input stream, causing the {@link #onDataAvailable} not to be called even once.
        while (true) {
            if (!parseCurrentItem())
                break;
        }
        completeExecutor.accept(context);
        request.getAsyncContext().complete();
    }

    @Override
    public void onError(Throwable t) {
        errorExecutor.accept(context, t);
    }
}
