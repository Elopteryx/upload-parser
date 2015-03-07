package com.elopteryx.paint.upload.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * A channel implementation which discards the data supplied.
 * Used by the parser if it doesn't have a channel to write to.
 * The purpose of this is to make the OnPartBegin callback
 * optional, which is useful for testing.
 */
class NullChannel implements WritableByteChannel {

    /*
     * Flag to determine whether the channel is closed or not.
     */
    private boolean open = true;

    @Override
    public int write(ByteBuffer src) throws IOException {
        int dataCount = src.remaining();
        byte[] buf = new byte[dataCount];
        src.get(buf, 0, dataCount);
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
