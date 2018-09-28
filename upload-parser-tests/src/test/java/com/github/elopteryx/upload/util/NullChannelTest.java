package com.github.elopteryx.upload.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

class NullChannelTest {

    @Test
    void read_from_open_channel() throws Exception {
        final var channel = new NullChannel();
        assertEquals(-1, channel.read(ByteBuffer.allocate(0)));
    }

    @Test
    void read_from_closed_channel() {
        final var channel = new NullChannel();
        assertTrue(channel.isOpen());
        channel.close();
        assertFalse(channel.isOpen());
        assertThrows(ClosedChannelException.class, () -> channel.read(ByteBuffer.allocate(0)));
    }

    @Test
    void write_to_open_channel() throws IOException {
        final var channel = new NullChannel();
        channel.write(ByteBuffer.allocate(0));
    }

    @Test
    void write_to_closed_channel() {
        final var channel = new NullChannel();
        assertTrue(channel.isOpen());
        channel.close();
        assertFalse(channel.isOpen());
        assertThrows(ClosedChannelException.class, () -> channel.write(ByteBuffer.allocate(0)));
    }

}