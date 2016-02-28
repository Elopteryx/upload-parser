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

package com.github.elopteryx.upload.errors;

/**
 * Exception thrown when there is a maximum size limit set for the whole
 * request and it is exceeded for the first time.
 */
public class RequestSizeException extends UploadSizeException {

    /**
     * Public constructor.
     * @param message The message of the exception
     * @param actual The known size at the time of the exception in bytes
     * @param permitted The maximum permitted size in bytes
     */
    public RequestSizeException(String message, long actual, long permitted) {
        super(message, actual, permitted);
    }
}
