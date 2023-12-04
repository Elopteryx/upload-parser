package com.github.elopteryx.upload.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

/**
 * A channel implementation which reads the ByteBuffer
 * data from the given InputStream instance.
 *
 * <p>This implementation differs from the one returned
 * in {@link java.nio.channels.Channels#newChannel(InputStream)}
 * by one notable thing, it does not use a temporary buffer, because
 * it will not handle read-only or direct ByteBuffers.</p>
 *
 * <p>The channel honors the close contract, it cannot be used after closing.</p>
 */
public class InputStreamBackedChannel implements ReadableByteChannel {

    /**
     * Flag to determine whether the channel is closed or not.
     */
    private boolean open = true;

    /**
     * The stream the channel will read from.
     */
    private final InputStream inputStream;

    /**
     * Public constructor.
     * @param inputStream The input stream
     */
    public InputStreamBackedChannel(final InputStream inputStream) {
        this.inputStream = Objects.requireNonNull(inputStream);
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        if (!open) {
            throw new ClosedChannelException();
        }
        if (dst.isDirect() || dst.isReadOnly()) {
            throw new IllegalArgumentException("The buffer cannot be direct or read-only!");
        }
        final var buf = dst.array();
        final var offset = dst.position();
        final var len = dst.remaining();
        final var read = inputStream.read(buf, offset, len);
        dst.position(offset + read);
        return read;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        open = false;
    }
}
