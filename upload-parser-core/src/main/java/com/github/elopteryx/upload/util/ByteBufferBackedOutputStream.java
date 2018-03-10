package com.github.elopteryx.upload.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An output stream implementation which writes to the given byte buffer.
 * The stream will not copy bytes to a temporary buffer, therefore read-only
 * and direct buffers are not supported.
 */
class ByteBufferBackedOutputStream extends OutputStream {

    /**
     * The byte buffer. Cannot be read-only or direct.
     */
    private final ByteBuffer buffer;

    /**
     * Flag to determine whether the channel is closed or not.
     */
    private boolean open = true;

    /**
     * Public constructor.
     * @param buffer The byte buffer
     */
    public ByteBufferBackedOutputStream(ByteBuffer buffer) {
        this.buffer = Objects.requireNonNull(buffer);
        if (buffer.isDirect() || buffer.isReadOnly()) {
            throw new IllegalArgumentException("The buffer cannot be direct or read-only!");
        }
    }

    @Override
    public void write(int byteToWrite) throws IOException {
        if (!open) {
            throw new IOException("The stream was closed!");
        }
        buffer.put((byte) byteToWrite);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        if (!open) {
            throw new IOException("The stream was closed!");
        }
        buffer.put(bytes, off, len);
    }

    @Override
    public void close() throws IOException {
        open = false;
    }
}
