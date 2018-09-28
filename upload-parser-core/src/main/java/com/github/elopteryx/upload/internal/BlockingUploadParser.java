/*
 * Copyright (C) 2016 Adam Forgacs
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

import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.errors.MultipartException;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * The blocking implementation of the parser. This parser can be used to perform a
 * blocking parse, whether the servlet supports async mode or not.
 */
public class BlockingUploadParser extends AbstractUploadParser {

    /**
     * The request object.
     */
    private final HttpServletRequest request;

    /**
     * The stream to read.
     */
    protected InputStream inputStream;

    public BlockingUploadParser(final HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Sets up the necessary objects to start the parsing. Depending upon
     * the environment the concrete implementations can be very different.
     * @throws IOException If an error occurs with the IO
     */
    private void init() throws IOException {
        init(request);
        inputStream = request.getInputStream();
    }

    /**
     * Performs a full parsing and returns the used context object.
     * @return The upload context
     * @throws IOException If an error occurred with the I/O
     * @throws ServletException If an error occurred with the servlet
     */
    public UploadContext doBlockingParse() throws IOException, ServletException {
        init();
        try {
            blockingRead();
            if (requestCallback != null) {
                requestCallback.onRequestComplete(context);
            }
        } catch (final Exception e) {
            if (errorCallback != null) {
                errorCallback.onError(context, e);
            }
        }
        return context;
    }

    /**
     * Reads everything from the input stream in a blocking mode. It will
     * throw an exception if the data is malformed, for example
     * it is not closed with the proper characters.
     * @throws IOException If an error occurred with the I/O
     */
    protected void blockingRead() throws IOException {
        while (true) {
            final var count = inputStream.read(dataBuffer.array());
            if (count == -1) {
                if (parseState.isComplete()) {
                    break;
                } else {
                    throw new MultipartException("Stream ended unexpectedly!");
                }
            } else if (count > 0) {
                checkRequestSize(count);
                dataBuffer.position(0);
                dataBuffer.limit(count);
                parseState.parse(dataBuffer);
            }
        }
    }
}
