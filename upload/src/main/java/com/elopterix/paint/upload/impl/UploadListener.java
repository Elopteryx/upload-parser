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
package com.elopterix.paint.upload.impl;

import com.elopterix.paint.upload.UploadParser;
import com.elopterix.paint.upload.exceptions.PartSizeException;
import com.elopterix.paint.upload.exceptions.RequestSizeException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * Asynchronous upload listener. Called by the servlet
 * container whenever data is available.
 */
public class UploadListener extends UploadParser implements ReadListener {

    /**
     * Minimal buffer size.
     */
    private static final int MIN_BUFFER_SIZE = 4096;
    
    private ByteBuffer buffer;

    private ServletInputStream servletInputStream;

    private UploadIterator iterator;

    private UploadContextImpl context;

    private PartStreamImpl currentItem;

    private WritableByteChannel writableChannel;

    public UploadListener(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public void setup()
            throws IOException {
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
        iterator = new UploadIterator(request);
        if (iterator.hasNext())
            currentItem = iterator.next();
        context = new UploadContextImpl(request, response);
        context.reset(currentItem);
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
        long requestSize = iterator.getBytesRead();
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

    /**
     * Parses the servlet stream once. Will switch to a new item
     * if the current one is fully read.
     *
     * @return Whether it should be called again
     * @throws IOException
     */
    private boolean parseCurrentItem() throws IOException {
        ReadableByteChannel channel = currentItem.getChannel();
        int read = channel.read(buffer);
        if (read != -1)
            checkSize(read);
        if (context.isBuffering() && (context.getPartBytesRead() >= sizeThreshold || read == -1)) {
            writableChannel = Objects.requireNonNull(partValidator.apply(context, buffer.asReadOnlyBuffer()));
            context.finishBuffering();
        }
        if(!context.isBuffering()) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                writableChannel.write(buffer);
            }
            buffer.compact();
        }
        if (read == -1) {
            partExecutor.accept(context, writableChannel);
            if (iterator.hasNext()) {
                currentItem = iterator.next();
                writableChannel = null;
                context.reset(currentItem);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
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
