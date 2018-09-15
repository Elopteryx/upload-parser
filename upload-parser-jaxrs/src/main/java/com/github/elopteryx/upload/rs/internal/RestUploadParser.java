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

package com.github.elopteryx.upload.rs.internal;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import com.github.elopteryx.upload.errors.RequestSizeException;
import com.github.elopteryx.upload.internal.BlockingUploadParser;
import com.github.elopteryx.upload.internal.Headers;
import com.github.elopteryx.upload.internal.MultipartParser;
import com.github.elopteryx.upload.internal.PartStreamImpl;
import com.github.elopteryx.upload.internal.UploadContextImpl;
import com.github.elopteryx.upload.rs.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A subclass of the blocking parser. It doesn't have a dependency
 * on the servlet request and can be initialized from the header values.
 * This makes it ideal for a Jax-Rs environment, to be used in a
 * message body reader.
 */
public class RestUploadParser extends BlockingUploadParser {

    /**
     * Public constructor.
     */
    public RestUploadParser() {
        super(null);
    }

    /**
     * Initializes the parser from the given parameters and performs
     * a blocking parse.
     * @param contentLength The length of the request
     * @param mimeType The content type of the request
     * @param encoding The character encoding of the request
     * @param stream The request stream
     * @return The multipart object, representing the request
     * @throws IOException If an error occurred with the I/O
     */
    public MultiPartImpl doBlockingParse(long contentLength, String mimeType, String encoding, InputStream stream) throws IOException {
        if (maxRequestSize > -1) {
            if (contentLength > maxRequestSize) {
                throw new RequestSizeException("The size of the request ("
                        + contentLength
                        + ") is greater than the allowed size (" + maxRequestSize + ")!",
                        contentLength, maxRequestSize);
            }
        }

        checkBuffer = ByteBuffer.allocate(sizeThreshold);
        context = new UploadContextImpl(null, null);
        dataBuffer = ByteBuffer.allocate(maxBytesUsed / 2);

        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = Headers.extractBoundaryFromHeader(mimeType);
            if (boundary == null) {
                throw new IllegalArgumentException("Could not find boundary in multipart request with ContentType: "
                        + mimeType
                        + ", multipart data will not be available");
            }
            var charset = encoding != null ? Charset.forName(encoding) : ISO_8859_1;
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), maxBytesUsed, charset);

            inputStream = stream;
        }
        blockingRead();
        List<Part> parts = context.getPartStreams()
                .stream()
                .map(PartStreamImpl.class::cast)
                .map(PartImpl::new)
                .collect(Collectors.toList());
        return new MultiPartImpl(parts, requestSize);
    }
}
