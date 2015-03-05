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

import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.errors.MultipartException;
import com.elopteryx.paint.upload.errors.RequestSizeException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

/**
 * The blocking implementation of the parser. This parser can be used to perform a
 * blocking parse, whether the servlet supports async mode or not.
 */
public class BlockingUploadParser extends UploadParser<BlockingUploadParser> {

    /**
     * The request object.
     */
    protected HttpServletRequest request;

    protected InputStream inputStream;

    public BlockingUploadParser(@Nonnull HttpServletRequest request) {
        this.request = requireNonNull(request);
    }

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

        inputStream = request.getInputStream();
    }

    /**
     * The parser begins parsing the request stream. This is a blocking method,
     * the method will not finish until the upload process finished, either
     * successfully or not.
     * @return The upload context
     * @throws IOException If an error occurred with the servlet stream
     */
    public UploadContext parse() throws IOException {
        init();
        try {
            while(true) {
                int c = inputStream.read(buf);
                if (c == -1) {
                    if (!parseState.isComplete())
                        throw new MultipartException();
                    else
                        break;
                } else if(c > 0) {
                    checkRequestSize(c);
                    parseState.parse(ByteBuffer.wrap(buf, 0, c));
                }
            }
            if(requestCallback != null)
                requestCallback.onRequestComplete(context);
        } catch (Exception e) {
            if(errorCallback != null)
                errorCallback.onError(context, e);
        }
        return context;
    }
}
