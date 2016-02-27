/*
 * Copyright (C) 2015 Adam Forgacs
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

import com.github.elopteryx.upload.OnPartBegin;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * A channel implementation which provides no data and discards the data supplied.
 * Used by the parser if it doesn't have a channel to write to.
 * The purpose of this is to make the {@link OnPartBegin} callback
 * optional, which is useful for testing.
 */
public class NullChannel implements ReadableByteChannel, WritableByteChannel {

    /**
     * Flag to determine whether the channel is closed or not.
     */
    private boolean open = true;

    @Override
    public int read(@Nonnull ByteBuffer dst) throws IOException {
        if (!open) {
            throw new ClosedChannelException();
        }
        return -1;
    }

    @Override
    public int write(@Nonnull ByteBuffer src) throws IOException {
        int remaining = src.remaining();
        src.position(src.limit());
        return remaining;
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
