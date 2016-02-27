package com.github.elopteryx.upload.util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An input stream implementation which reads from the given byte buffer.
 * The stream will not copy bytes to a temporary buffer, therefore read-only
 * and direct buffers are not supported.
 */
public class ByteBufferBackedInputStream extends InputStream {

    /**
     * The byte buffer. Cannot be read-only or direct.
     */
    private final ByteBuffer buffer;

    /**
     * Public constructor.
     * @param buffer The byte buffer
     */
    public ByteBufferBackedInputStream(@Nonnull ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int read() throws IOException {
        if (!buffer.hasRemaining()) {
            return -1;
        }
        return buffer.get() & 0xFF;
    }

    @Override
    public int read(@Nonnull byte[] bytes, int off, int len) throws IOException {
        if (!buffer.hasRemaining()) {
            return -1;
        }

        len = Math.min(len, buffer.remaining());
        buffer.get(bytes, off, len);
        return len;
    }
}
