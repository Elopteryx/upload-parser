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
    public ByteBufferBackedInputStream(ByteBuffer buffer) {
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
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!buffer.hasRemaining()) {
            return -1;
        }

        len = Math.min(len, buffer.remaining());
        buffer.get(bytes, off, len);
        return len;
    }
}
