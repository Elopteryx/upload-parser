package com.github.elopteryx.upload.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * A channel implementation which writes the ByteBuffer data
 * to the given OutputStream instance.
 *
 * <p>This implementation differs from the one returned
 * in {@link java.nio.channels.Channels#newChannel(OutputStream)} Channels.newChannel
 * by one notable thing, it does not use a temporary buffer, because
 * it will not receive read-only ByteBuffers.</p>
 */
public class StreamBackedChannel implements WritableByteChannel {

    /**
     * Flag to determine whether the channel is closed or not.
     */
    private boolean open = true;

    /**
     * The stream the channel will write to.
     */
    private final OutputStream outputStream;

    public StreamBackedChannel(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        byte[] buf = src.array();
        int offset = src.position();
        int len = src.remaining();
        outputStream.write(buf, offset, len);
        src.position(offset + len);
        return len;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
        open = false;
    }
}
