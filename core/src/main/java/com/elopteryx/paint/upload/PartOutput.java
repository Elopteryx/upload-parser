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

import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * A value holder class, allowing the caller to provide
 * various output objects, like a byte channel or an output stream.
 */
public class PartOutput {

    /**
     * The value object.
     */
    private final Object value;

    /**
     * Protected constructor, no need for public access.
     * The parser will use the given object here, which is why using
     * the static factory methods is encouraged. Passing an invalid
     * object will terminate the upload process.
     * @param value The value object.
     */
    protected PartOutput(Object value) {
        this.value = value;
    }

    /**
     * Returns whether it is safe to retrieve the value object
     * with the class parameter.
     * @param clazz The class type to check
     * @param <T> Type parameter
     * @return Whether it is safe to cast or not
     */
    public <T> boolean safeToCast(Class<T> clazz) {
        return value != null && clazz.isAssignableFrom(value.getClass());
    }

    /**
     * Retrieves the value object, casting it to the
     * given type.
     * @param clazz The class to cast
     * @param <T> Type parameter
     * @return The stored value object
     */
    public <T> T unwrap(Class<T> clazz) {
        return clazz.cast(value);
    }

    /**
     * Creates a new instance from the given channel object.
     * @param byteChannel A channel which can be used for writing
     * @return A new PartOutput instance
     */
    public static PartOutput from(@Nonnull WritableByteChannel byteChannel) {
        return new PartOutput(byteChannel);
    }

    /**
     * Creates a new instance from the given stream object.
     * @param outputStream A stream which can be used for writing
     * @return A new PartOutput instance
     */
    public static PartOutput from(@Nonnull OutputStream outputStream) {
        return new PartOutput(outputStream);
    }

    /**
     * Creates a new instance from the given path object.
     * @param path A file path which can be used for writing
     * @return A new PartOutput instance
     */
    public static PartOutput from(@Nonnull Path path) {
        return new PartOutput(path);
    }
}
