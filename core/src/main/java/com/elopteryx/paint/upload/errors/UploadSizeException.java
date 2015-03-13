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

package com.elopteryx.paint.upload.errors;

/**
 * Base class for the size related exceptions.
 */
public abstract class UploadSizeException extends RuntimeException {

    /**
     * The known size.
     */
    private final long actual;

    /**
     * The maximum permitted size.
     */
    private final long permitted;

    /**
     * Exception thrown when there is a maximum size limit and it is exceeded for the first time.
     * @param message The message of the exception
     * @param actual The known size at the time of the exception in bytes
     * @param permitted The maximum permitted size in bytes
     */
    UploadSizeException(String message, long actual, long permitted) {
        super(message);
        this.actual = actual;
        this.permitted = permitted;
    }

    /**
     * Returns the actual size.
     *
     * @return The actual size.
     */
    public long getActualSize() {
        return actual;
    }

    /**
     * Returns the permitted size.
     *
     * @return The permitted size.
     */
    public long getPermittedSize() {
        return permitted;
    }
}
