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
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Builder class. Provides a fluent API for the users to
 * customize the parsing process.
 */
public class Upload {

    /**
     * Part of HTTP content type header.
     */
    private static final String MULTIPART = "multipart/";

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