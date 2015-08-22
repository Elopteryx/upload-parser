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

package com.github.elopteryx.upload.rs;

import com.github.elopteryx.upload.OnPartBegin;
import com.github.elopteryx.upload.OnPartEnd;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.internal.Headers;
import com.github.elopteryx.upload.rs.internal.MultiPartImpl;
import com.github.elopteryx.upload.rs.internal.RestUploadParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

/**
 * This class is a message body reader for multipart requests. It works like the blocking
 * upload parser. It provides a default configuration which creates temporary
 * files to store uploaded file data and stores each normal form field in a
 * {@link ByteArrayOutputStream} instance.
 * Other readers can extend this and implement the callback interfaces,
 * which are the part begin and end callbacks. You must manually register this
 * or its subclass to be used by the Jax-Rs runtime.
 *
 * <p>The reader does not support the request callback, because that code can go
 * into the controller method instead and the error callback, because there are
 * exception mappers for that.</p>
 *
 * <p>The reader can inject the following parameters:</p>
 *
 * <ul>
 * <li>{@link MultiPart}</li>
 * <li>{@link Part} with a valid {@link UploadParam} annotation</li>
 * <li>{@link Collection} of {@link Part} instances</li>
 * <li>{@link List} of {@link Part} instances</li>
 * </ul>
 *
 * <p>Other parameters are not supported by the reader.</p>
 */
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class UploadReader implements MessageBodyReader<Object>, OnPartBegin, OnPartEnd {

    /**
     * The parser object. Does not support async parsing.
     */
    private final RestUploadParser parser = new RestUploadParser();

    /**
     * The object representing the multipart message.
     */
    private MultiPart multiPart;

    /**
     * Public constructor.
     */
    public UploadReader() {
        parser.setPartBeginCallback(this);
        parser.setPartEndCallback(this);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.getType().equals("multipart");
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        if (multiPart == null) {
            parse(annotations, httpHeaders, entityStream);
        }
        if (MultiPart.class.equals(type)) {
            return multiPart;
        } else if (Part.class.equals(type)) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof UploadParam) {
                    UploadParam formParam = (UploadParam)annotation;
                    return providePart(formParam.value());
                }
            }
            return null;
        } else if (Collection.class.equals(type) || List.class.equals(type)) {
            if (((ParameterizedType) genericType).getActualTypeArguments()[0].equals(Part.class)) {
                return multiPart.getParts();
            } else {
                return null;
            }
        }
        return null;
    }

    private void parse(Annotation[] annotations, MultivaluedMap<String, String> httpHeaders,
                       InputStream entityStream) throws IOException {
        for (Annotation annotation : annotations) {
            if (annotation instanceof UploadConfig) {
                UploadConfig config = (UploadConfig)annotation;
                parser.setSizeThreshold(config.sizeThreshold());
                parser.setMaxPartSize(config.maxPartSize());
                parser.setMaxRequestSize(config.maxRequestSize());
                break;
            }
        }

        long requestSize = Long.valueOf(httpHeaders.getFirst(Headers.CONTENT_LENGTH));
        String mimeType = httpHeaders.getFirst(Headers.CONTENT_TYPE);
        String encodingHeader = httpHeaders.getFirst(Headers.CONTENT_ENCODING);
        MultiPartImpl multiPart = parser.doBlockingParse(requestSize, mimeType, encodingHeader, entityStream);
        multiPart.setHeaders(httpHeaders);
        this.multiPart = multiPart;
    }

    /**
     * Finds the appropriate part for the given form name.
     * @param name The form name.
     * @return The matched part or null if no part exists with that name
     */
    @Nullable
    private Part providePart(String name) {
        for (Part part : multiPart.getParts()) {
            if (name.equals(part.getName())) {
                return part;
            }
        }
        return null;
    }

    @Override
    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
        PartOutput output;
        if (context.getCurrentPart().isFile()) {
            output = PartOutput.from(Files.createTempFile(null, ".tmp"));
        } else {
            output = PartOutput.from(new ByteArrayOutputStream());
        }
        return output;
    }

    @Override
    public void onPartEnd(UploadContext context) throws IOException {
        // No need to do anything.
    }
}
