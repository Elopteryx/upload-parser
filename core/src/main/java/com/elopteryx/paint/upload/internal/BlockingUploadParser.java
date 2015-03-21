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

package com.elopteryx.paint.upload.internal;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.errors.MultipartException;
import com.elopteryx.paint.upload.errors.RequestSizeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import javax.annotation.Nullable;
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

    protected InputStream inputStream;

    public BlockingUploadParser(@Nullable HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Sets up the necessary objects to start the parsing. Depending upon
     * the environment the concrete implementations can be very different.
     * @throws IOException If an error occurs with the IO
     */
    private void init() throws IOException {

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

        String mimeType = request.getHeader(PartStreamHeaders.CONTENT_TYPE);
        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = PartStreamHeaders.extractBoundaryFromHeader(mimeType);
            if (boundary == null) {
                throw new IllegalArgumentException("Could not find boundary in multipart request with ContentType: "
                        + mimeType
                        + ", multipart data will not be available");
            }
            String encodingHeader = request.getCharacterEncoding();
            Charset charset = encodingHeader != null ? Charset.forName(encodingHeader) : ISO_8859_1;
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), charset);
        }

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
            while (true) {
                int count = inputStream.read(buf);
                if (count == -1) {
                    if (!parseState.isComplete()) {
                        throw new MultipartException();
                    } else {
                        break;
                    }
                } else if (count > 0) {
                    checkRequestSize(count);
                    parseState.parse(ByteBuffer.wrap(buf, 0, count));
                }
            }
            if (requestCallback != null) {
                requestCallback.onRequestComplete(context);
            }
        } catch (Exception e) {
            if (errorCallback != null) {
                errorCallback.onError(context, e);
            }
        }
        return context;
    }
}
