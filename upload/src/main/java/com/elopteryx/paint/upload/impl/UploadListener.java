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
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
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

    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    
    private ByteBuffer buffer;

    private ServletInputStream servletInputStream;

    private UploadContextImpl context;

    private PartStreamImpl currentItem;

    private WritableByteChannel writableChannel;
    
    private MultipartParser.ParseState parseState;

    private static final String defaultEncoding = "ISO-8859-1";
    

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

        servletInputStream = request.getInputStream();
        if (!request.isAsyncStarted())
            request.startAsync();
        servletInputStream.setReadListener(this);

        buffer = ByteBuffer.allocate(Math.max(MIN_BUFFER_SIZE, sizeThreshold));
        context = new UploadContextImpl(request, response);

        String mimeType = request.getHeader(Headers.CONTENT_TYPE);
        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = Headers.extractTokenFromHeader(mimeType, "boundary");
            if (boundary == null) {
                throw new RuntimeException("Could not find boundary in multipart request with ContentType: "+mimeType+", multipart data will not be available");
            }
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), request.getCharacterEncoding() != null ? request.getCharacterEncoding() : defaultEncoding);
        }
        readableByteChannel = Channels.newChannel(servletInputStream);
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
    private void checkSize(int additional) {
        long partSize = context.incrementAndGetPartBytes(additional);
        long requestSize = 0;//iterator.getBytesRead(); TODO
        if (maxPartSize > -1)
            if (partSize > maxPartSize)
                throw new PartSizeException("The size of the part (" + partSize +
                        ") is greater than the allowed size (" + maxPartSize + ")!", partSize, maxPartSize);
        if (maxRequestSize > -1) {
            if (requestSize > maxRequestSize)
                throw new RequestSizeException("The size of the request (" + requestSize +
                        ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
        }
    }

    private byte[] buf = new byte[8192];
    
    private ReadableByteChannel readableByteChannel;
    
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
            if (!parseState.isComplete()) {
                throw new MalformedMessageException();
            }
            //TODO this should never be 0, the input is always called when ready
        } else if (c != 0) {
            parseState.parse(ByteBuffer.wrap(buf, 0, c));
        }
        return !parseState.isComplete();
    }

    @Override
    public void beginPart(final PartStreamHeaders headers) {
        final String disposition = headers.getHeader(Headers.CONTENT_DISPOSITION);
        if (disposition != null) {
            if (disposition.startsWith("form-data")) {
                String fieldName = Headers.extractQuotedValueFromHeader(disposition, "name");
                String fileName = Headers.extractQuotedValueFromHeader(disposition, "filename");
                currentItem = new PartStreamImpl(fileName, fieldName, headers.getHeader(Headers.CONTENT_TYPE), headers);
                context.reset(currentItem);
            }
        }
    }

    @Override
    public void data(final ByteBuffer buffer) throws IOException {
        //TODO copy the buffered bytes
        int read = buffer.position();
        if (read != -1)
            checkSize(read);
        //TODO track the bytes for size threshold
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
