package com.elopteryx.paint.upload.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * A channel implementation which discards the data supplied.
 */
public class NullChannel implements WritableByteChannel {

    private boolean open = true;

    @Override
    public int write(ByteBuffer src) throws IOException {
        int dataCount = src.remaining();
        src.clear();
        return dataCount;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        open = false;
    }
}
