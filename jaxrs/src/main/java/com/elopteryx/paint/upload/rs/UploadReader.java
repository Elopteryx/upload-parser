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

package com.elopteryx.paint.upload.rs;

import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.internal.PartStreamHeaders;
import com.elopteryx.paint.upload.rs.internal.RestUploadParser;

import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

/**
 * This class is a message body reader for multipart requests. It works like the blocking
 * upload parser. Actual readers should extend this and implement the callback interfaces,
 * which are the part begin and end callbacks. No onRequestComplete, because that should go
 * into the controller method and no onError, because that should be handled by an
 * exception mapper.
 */
@Consumes(MediaType.MULTIPART_FORM_DATA)
public abstract class UploadReader implements MessageBodyReader<UploadContext>, OnPartBegin, OnPartEnd {

    private static final String CONTENT_LENGTH = "Content-Length";

    private static final String CONTENT_ENCODING = "Content-Encoding";

    /**
     * The parser object. Does not support async parsing.
     */
    private final RestUploadParser parser = new RestUploadParser();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.getType().equals("multipart");
    }

    @Override
    public UploadContext readFrom(Class<UploadContext> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        parser.setPartBeginCallback(this);
        parser.setPartEndCallback(this);

        for (Annotation annotation : annotations) {
            if (annotation instanceof UploadConfig) {
                UploadConfig config = (UploadConfig)annotation;
                parser.setSizeThreshold(config.sizeThreshold());
                parser.setMaxPartSize(config.maxPartSize());
                parser.setMaxRequestSize(config.maxRequestSize());
                break;
            }
        }

        long requestSize = Long.valueOf(httpHeaders.getFirst(CONTENT_LENGTH));
        String mimeType = httpHeaders.getFirst(PartStreamHeaders.CONTENT_TYPE);
        String encodingHeader = httpHeaders.getFirst(CONTENT_ENCODING);
        return parser.doBlockingParse(requestSize, mimeType, encodingHeader, entityStream);
    }
}
