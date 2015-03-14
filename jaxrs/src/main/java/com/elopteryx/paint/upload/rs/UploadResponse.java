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

import javax.annotation.Nonnull;
import javax.ws.rs.container.AsyncResponse;

/**
 * Jax-Rs specific version of the response wrapper. This class can be used
 * to wrap an AsyncResponse to retrieve it later if the instance reference
 * is not available.
 */
public class UploadResponse extends com.elopteryx.paint.upload.UploadResponse { //TODO UploadResponse shouldn't be a value holder, just return with the response object, and extend it

    /**
     * Public constructor.
     * @param value The value object.
     */
    public UploadResponse(Object value) {
        super(value);
    }

    /**
     * Creates a new instance from the given response object.
     * @param asyncResponse The Jax-Rs async response
     * @return A new UploadResponse instance
     */
    public static UploadResponse from(@Nonnull AsyncResponse asyncResponse) {
        return new UploadResponse(asyncResponse);
    }
}
