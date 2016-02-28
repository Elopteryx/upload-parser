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

package com.github.elopteryx.upload.util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * A channel implementation which writes the ByteBuffer data
 * to the given OutputStream instance.
 *
 * <p>This implementation differs from the one returned
 * in {@link java.nio.channels.Channels#newChannel(OutputStream)}
 * by one notable thing, it does not use a temporary buffer, because
 * it will not receive read-only ByteBuffers.</p>
 */
public class OutputStreamBackedChannel implements WritableByteChannel {

    /**
     * Flag to determine whether the channel is closed or not.
     */
    private boolean open = true;

    /**
     * The stream the channel will write to.
     */
    private final OutputStream outputStream;

    /**
     * Public constructor.
     * @param outputStream The output stream
     */
    public OutputStreamBackedChannel(@Nonnull OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public int write(@Nonnull ByteBuffer src) throws IOException {
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
