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

import com.elopteryx.paint.upload.impl.ValueHolder;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * A value holder class, allowing the caller to provide
 * different response objects depending upon the web
 * framework used.
 */
public class UploadResponse extends ValueHolder {

    /**
     * Public constructor. Only made public to allow more flexibility in certain
     * environments where the servlet response object is unavailable.
     * The parser will not use the given object, so it's safe to pass anything here.
     * @param value The value object.
     */
    public UploadResponse(Object value) {
        super(value);
    }

    /**
     * Creates a new instance from the given response object.
     * @param response The servlet response
     * @return A new UploadResponse instance
     */
    public static UploadResponse from(@Nonnull HttpServletResponse response) {
        return new UploadResponse(response);
    }
}
