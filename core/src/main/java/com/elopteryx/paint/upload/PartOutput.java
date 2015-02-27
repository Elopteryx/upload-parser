package com.elopteryx.paint.upload;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

/**
 * A value holder class, allowing the caller to provide
 * a byte channel or an output stream.
 */
public class PartOutput implements Closeable {

    /**
     * The value object
     */
    private Closeable value;

    private PartOutput() {
        // No need to allow public access
    }

    /**
     * Creates a new instance from the given channel object.
     * @param byteChannel A channel which can be used for writing
     * @return A new PartOutput instance
     */
    public static PartOutput from(@Nonnull WritableByteChannel byteChannel) {
        PartOutput partOutput = new PartOutput();
        partOutput.value = byteChannel;
        return partOutput;
    }

    /**
     * Creates a new instance from the given stream object.
     * @param outputStream A stream which can be used for writing
     * @return A new PartOutput instance
     */
    public static PartOutput from(@Nonnull OutputStream outputStream) {
        PartOutput partOutput = new PartOutput();
        partOutput.value = outputStream;
        return partOutput;
    }

    /**
     * Returns whether it is safe to retrieve the value object
     * with the class parameter.
     * @param clazz The class type to check
     * @param <T> Type parameter
     * @return Whether it is safe to cast or not
     */
    public <T> boolean safeToCast(Class<T> clazz) {
        return clazz.isAssignableFrom(value.getClass());
    }

    /**
     * Retrieves the value object, casting it to the
     * given type.
     * @param clazz The class to to cast
     * @param <T> Type parameter
     * @return The stored value object
     */
    public <T> T get(Class<T> clazz) {
        return clazz.cast(value);
    }

    /**
     * Closes the value object. Because all possible value
     * types are closeable this method can be used instead
     * without retrieving the actual object.
     * @throws IOException If an error occurred with closing
     */
    @Override
    public void close() throws IOException {
        value.close();
    }
}
