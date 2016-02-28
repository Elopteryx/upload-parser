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

package com.github.elopteryx.upload.rs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which can be put on the {@link MultiPart} parameter, to control the size constraints.
 * Adding them into an annotation will work like if they were passed into the parser using
 * the fluent API.
 */
@Target(value = { ElementType.PARAMETER, ElementType.FIELD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface UploadConfig {

    /**
     * The number of bytes that should be buffered before calling the part begin callback.
     * @return The amount to use.
     */
    int sizeThreshold() default 0;

    /**
     * The maximum size permitted for the parts. By default it is unlimited.
     * @return The amount to use.
     */
    long maxPartSize() default -1L;

    /**
     * The maximum size permitted for the complete request. By default it is unlimited.
     * @return The amount to use.
     */
    long maxRequestSize() default -1L;

}
