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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

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
     * Flag to determine whether the channel is closed or not.
     */
    private boolean open = true;

    /**
     * Public constructor.
     * @param buffer The byte buffer
     */
    public ByteBufferBackedInputStream(final ByteBuffer buffer) {
        this.buffer = Objects.requireNonNull(buffer);
        if (buffer.isDirect() || buffer.isReadOnly()) {
            throw new IllegalArgumentException("The buffer cannot be direct or read-only!");
        }
    }

    @Override
    public int available() throws IOException {
        return buffer.remaining();
    }

    @Override
    public int read() throws IOException {
        if (!open) {
            throw new IOException("The stream was closed!");
        }
        if (!buffer.hasRemaining()) {
            return -1;
        }
        return buffer.get() & 0xFF;
    }

    @Override
    public int read(final byte[] bytes, final int off, int len) throws IOException {
        if (!open) {
            throw new IOException("The stream was closed!");
        }
        if (!buffer.hasRemaining()) {
            return -1;
        }
        len = Math.min(len, buffer.remaining());
        buffer.get(bytes, off, len);
        return len;
    }

    @Override
    public void close() throws IOException {
        open = false;
    }
}
