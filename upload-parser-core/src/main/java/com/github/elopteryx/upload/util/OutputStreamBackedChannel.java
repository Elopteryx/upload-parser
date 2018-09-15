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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * A channel implementation which writes the ByteBuffer data
 * to the given OutputStream instance.
 *
 * <p>This implementation differs from the one returned
 * in {@link java.nio.channels.Channels#newChannel(OutputStream)}
 * by one notable thing, it does not use a temporary buffer, because
 * it will not handle read-only or direct ByteBuffers.</p>
 *
 * <p>The channel honors the close contract, it cannot be used after closing.</p>
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
    public OutputStreamBackedChannel(OutputStream outputStream) {
        this.outputStream = Objects.requireNonNull(outputStream);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!open) {
            throw new ClosedChannelException();
        }
        if (src.isDirect() || src.isReadOnly()) {
            throw new IllegalArgumentException("The buffer cannot be direct or read-only!");
        }
        var buf = src.array();
        var offset = src.position();
        var len = src.remaining();
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
