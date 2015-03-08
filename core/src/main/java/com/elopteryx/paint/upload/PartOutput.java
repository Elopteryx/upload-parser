package com.elopteryx.paint.upload;

import com.elopteryx.paint.upload.impl.ValueHolder;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;

/**
 * A value holder class, allowing the caller to provide
 * a byte channel or an output stream.
 */
public class PartOutput extends ValueHolder implements Closeable {

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
     * Creates a new instance from the given path object.
     * @param path A stream which can be used for writing
     * @return A new PartOutput instance
     */
    public static PartOutput from(@Nonnull Path path) {
        PartOutput partOutput = new PartOutput();
        partOutput.value = path;
        return partOutput;
    }

    /**
     * Closes the value object. Because all possible value
     * types are closeable this method can be used instead
     * without retrieving the actual object.
     * @throws IOException If an error occurred with closing
     */
    @Override
    public void close() throws IOException {
        //TODO close the used writablechannel, and remove this
        if(value != null)
            ((Closeable)value).close();
    }
}
