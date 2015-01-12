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
package com.elopteryx.paint.upload;

import com.elopteryx.paint.upload.impl.UploadListener;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Builder class. Provides a fluent API for the users to
 * customize the parsing process.
 */
public abstract class UploadParser {
    /**
     * The type of the HTTP request.
     */
    private static final String POST_METHOD = "POST";

    /**
     * Part of HTTP content type header.
     */
    private static final String MULTIPART = "multipart/";

    /**
     * The request object.
     */
    protected final HttpServletRequest request;

    /**
     * The response object.
     */
    protected final HttpServletResponse response;

    /**
     * The part validator, called at the beginning of each part parsing. Mandatory.
     */
    protected BiFunction<UploadContext, ByteBuffer, WritableByteChannel> partValidator;

    /**
     * The part executor, called at the end of each part parsing. Mandatory.
     */
    protected BiConsumer<UploadContext, WritableByteChannel> partExecutor;

    /**
     * The completion executor, called after every part has been processed. Optional.
     */
    protected Consumer<UploadContext> completeExecutor = context -> {
    };

    /**
     * The error executor, called when an error occurred. Optional.
     */
    protected BiConsumer<UploadContext, Throwable> errorExecutor = (context, t) -> t.printStackTrace();

    /**
     * The number of bytes that should be buffered before calling the validation.
     */
    protected int sizeThreshold;

    /**
     * The maximum size permitted for the parts. By default it is unlimited.
     */
    protected long maxPartSize = -1;

    /**
     * The maximum size permitted for the complete request. By default it is unlimited.
     */
    protected long maxRequestSize = -1;

    protected UploadParser(HttpServletRequest request, HttpServletResponse response) {
        this.request = Objects.requireNonNull(request);
        this.response = Objects.requireNonNull(response);
    }

    public static boolean isMultipart(HttpServletRequest request) {
        return POST_METHOD.equalsIgnoreCase(request.getMethod()) && request.getContentType() != null &&
                request.getContentType().toLowerCase(Locale.ENGLISH).startsWith(MULTIPART);
    }

    public static UploadParser newParser(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response)
            throws ServletException {
        if (!isMultipart(request))
            throw new ServletException("Not a multipart request!");
        return new UploadListener(request, response);
    }

    public UploadParser onPartStart(@Nonnull BiFunction<UploadContext, ByteBuffer, WritableByteChannel> partValidator) {
        this.partValidator = Objects.requireNonNull(partValidator);
        return this;
    }

    public UploadParser onPartFinish(@Nonnull BiConsumer<UploadContext, WritableByteChannel> partExecutor) {
        this.partExecutor = Objects.requireNonNull(partExecutor);
        return this;
    }

    public UploadParser onComplete(@Nonnull Consumer<UploadContext> completeExecutor) {
        this.completeExecutor = Objects.requireNonNull(completeExecutor);
        return this;
    }

    public UploadParser onError(@Nonnull BiConsumer<UploadContext, Throwable> errorExecutor) {
        this.errorExecutor = Objects.requireNonNull(errorExecutor);
        return this;
    }

    public UploadParser sizeThreshold(@Nonnegative int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
        return this;
    }

    public UploadParser maxPartSize(@Nonnegative long maxPartSize) {
        this.maxPartSize = maxPartSize;
        return this;
    }

    public UploadParser maxRequestSize(@Nonnegative long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
        return this;
    }

    /**
     * Sets up the necessary objects to start the parsing. The
     * servlet container will be calling the read listener
     * whenever data is available.
     */
    public abstract void setup()
            throws IOException;
}