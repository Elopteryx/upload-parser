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
package com.elopteryx.paint.upload;

import com.elopteryx.paint.upload.impl.AsyncUploadParser;
import com.elopteryx.paint.upload.impl.BlockingUploadParser;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * The public API class for the library. Provides a fluent API for the users to
 * customize the parsing process.
 */
@SuppressWarnings("unchecked")
public class UploadParser<T extends UploadParser<T>> {

    /**
     * Part of HTTP content type header.
     */
    private static final String MULTIPART = "multipart/";

    /**
     * The response object.
     */
    protected UploadResponse uploadResponse;

    /**
     * The part begin callback, called at the beginning of each part parsing. Mandatory.
     */
    protected OnPartBegin partBeginCallback;

    /**
     * The part end callback, called at the end of each part parsing. Mandatory.
     */
    protected OnPartEnd partEndCallback;

    /**
     * The request callback, called after every part has been processed. Optional.
     */
    protected OnRequestComplete requestCallback;

    /**
     * The error callback, called when an error occurred. Optional.
     */
    protected OnError errorCallback;

    /**
     * The number of bytes that should be buffered before calling the part begin callback.
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

    /**
     * Sets a callback for each part, called at the beginning.
     * @param partBeginCallback An object or lambda expression
     * @return The parser will return itself
     */
    public T onPartBegin(OnPartBegin partBeginCallback) {
        this.partBeginCallback = partBeginCallback;
        return (T) this;
    }

    /**
     * Sets a callback for each part, called at the end.
     * @param partEndCallback An object or lambda expression
     * @return The parser will return itself
     */
    public T onPartEnd(OnPartEnd partEndCallback) {
        this.partEndCallback = partEndCallback;
        return (T) this;
    }

    /**
     * Sets a callback for the request, called after each part is processed.
     * @param requestCallback An object or lambda expression
     * @return The parser will return itself
     */
    public T onRequestComplete(OnRequestComplete requestCallback) {
        this.requestCallback = requestCallback;
        return (T) this;
    }

    /**
     * Sets a callback for the errors, called if any error occurs.
     * @param errorCallback An object or lambda expression
     * @return The parser will return itself
     */
    public T onError(OnError errorCallback) {
        this.errorCallback = errorCallback;
        return (T) this;
    }

    /**
     * Sets the servlet response object. This is only necessary to allow
     * access to it during the stages of the parsing. Note that if the
     * declaration of the custom functions are in the method which has
     * the response object as a parameter then this method can be skipped
     * and the parameter reference can be used instead.
     * @param uploadResponse The response wrapper
     * @return The parser will return itself
     */
    public T withResponse(@Nonnull UploadResponse uploadResponse) {
        this.uploadResponse = uploadResponse;
        return (T) this;
    }

    /**
     * Sets the amount of bytes to buffer in the memory, before
     * calling the part end callback.
     * @param sizeThreshold The amount to use
     * @return The parser will return itself
     */
    public T sizeThreshold(@Nonnegative int sizeThreshold) {
        this.sizeThreshold = Math.max(sizeThreshold, 0);
        return (T) this;
    }

    /**
     * Sets the maximum allowed size for each part. Exceeding this
     * will result in a {@link com.elopteryx.paint.upload.errors.PartSizeException} exception.
     * @param maxPartSize The amount to use
     * @return The parser will return itself
     */
    public T maxPartSize(@Nonnegative long maxPartSize) {
        this.maxPartSize = Math.max(maxPartSize, -1);
        return (T) this;
    }

    /**
     * Sets the maximum allowed size for the request. Exceeding this
     * will result in a {@link com.elopteryx.paint.upload.errors.RequestSizeException} exception.
     * @param maxRequestSize The amount to use
     * @return The parser will return itself
     */
    public T maxRequestSize(@Nonnegative long maxRequestSize) {
        this.maxRequestSize = Math.max(maxRequestSize, -1);
        return (T) this;
    }

    /**
     * Utility method which can be used to check whether the request
     * should be processed by this parser or not.
     * @param request The servlet request
     * @return Whether the request is a proper multipart request
     */
    @CheckReturnValue
    public static boolean isMultipart(@Nonnull HttpServletRequest request) {
        return request.getContentType() != null && request.getContentType().toLowerCase(Locale.ENGLISH).startsWith(MULTIPART);
    }

    /**
     * Returns an async parser implementation, allowing the caller to set configuration.
     * @param request The servlet request
     * @return A parser object
     * @throws ServletException If the parameters are invalid
     */
    @CheckReturnValue
    public static AsyncUploadParser newAsyncParser(@Nonnull HttpServletRequest request) throws ServletException {
        if (!isMultipart(request))
            throw new ServletException("Not a multipart request!");
        return new AsyncUploadParser(request);
    }

    /**
     * Returns a blocking parser implementation, allowing the caller to set configuration.
     * @param request The servlet request
     * @return A parser object
     * @throws ServletException If the parameters are invalid
     */
    @CheckReturnValue
    public static BlockingUploadParser newBlockingParser(@Nonnull HttpServletRequest request) throws ServletException {
        if (!isMultipart(request))
            throw new ServletException("Not a multipart request!");
        return new BlockingUploadParser(request);
    }
}