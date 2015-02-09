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

import com.elopteryx.paint.upload.errors.MultipartException;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The asynchronous implementation of the parser. If the calling servlet supports
 * async mode, then the created upload parser will be an instance of this class.
 * Implements the listener interface. Called by the servlet container whenever data is available.
 *
 * This class is only public to serve as an entry point in the implementation package, users
 * should not need to directly depend on this class.
 */
public class AsyncUploadParser extends UploadParserImpl implements ReadListener {

    public AsyncUploadParser(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public void setup() throws IOException {
        super.setup();
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
     * Parses the servlet stream once. Will switch to a new item
     * if the current one is fully read.
     *
     * @return Whether it should be called again
     * @throws IOException if an I/O related error has occurred during processing
     */
    private boolean parseCurrentItem() throws IOException {
        int c = servletInputStream.read(buf);
        if (c == -1) {
            if (!parseState.isComplete())
                throw new MultipartException();
        } else {
            checkRequestSize(c);
            parseState.parse(ByteBuffer.wrap(buf, 0, c));
        }
        return !parseState.isComplete();
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
            if(requestCallback != null)
                requestCallback.onRequestComplete(context);
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
        if(errorCallback != null)
            errorCallback.onError(context, t);
    }
}
