package com.github.elopteryx.upload.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

class InputStreamBackedChannelTest {

    private static final String TEST_TEXT = "Test text.";

    @Test
    void read_the_string() throws Exception {
        final var stream = new ByteArrayInputStream(TEST_TEXT.getBytes());
        final var channel = new InputStreamBackedChannel(stream);
        final var buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
        assertEquals(TEST_TEXT, new String(buffer.array(), 0, buffer.position()));
    }

    @Test
    void try_to_read_to_direct_buffer() {
        final var stream = new ByteArrayInputStream(TEST_TEXT.getBytes());
        final var channel = new InputStreamBackedChannel(stream);
        final var buffer = ByteBuffer.allocateDirect(1024);
        assertThrows(IllegalArgumentException.class, () -> channel.read(buffer));
    }

    @Test
    void try_to_read_to_read_only_buffer() {
        final var stream = new ByteArrayInputStream(TEST_TEXT.getBytes());
        final var channel = new InputStreamBackedChannel(stream);
        final var buffer = ByteBuffer.allocateDirect(1024).asReadOnlyBuffer();
        assertThrows(IllegalArgumentException.class, () -> channel.read(buffer));
    }

    @Test
    void open_and_close_and_try_to_read() throws IOException {
        final var stream = new ByteArrayInputStream(TEST_TEXT.getBytes());
        final var channel = new InputStreamBackedChannel(stream);
        assertTrue(channel.isOpen());
        channel.close();
        assertThrows(ClosedChannelException.class, () -> channel.read(ByteBuffer.allocate(0)));
    }

}