package com.github.elopteryx.upload.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

class OutputStreamBackedChannelTest {

    private static final String TEST_TEXT = "Test text.";

    @Test
    void write_the_string() throws IOException {
        var stream = new ByteArrayOutputStream(1024);
        var channel = new OutputStreamBackedChannel(stream);
        var buffer = ByteBuffer.wrap(TEST_TEXT.getBytes());
        channel.write(buffer);
        assertEquals(TEST_TEXT, new String(buffer.array(), 0, buffer.position()));
    }

    @Test
    void try_to_write_from_direct_buffer() {
        var stream = new ByteArrayOutputStream(1024);
        var channel = new OutputStreamBackedChannel(stream);
        var buffer = ByteBuffer.allocateDirect(0);
        assertThrows(IllegalArgumentException.class, () -> channel.write(buffer));
    }

    @Test
    void try_to_write_from_read_only_buffer() throws Exception {
        var stream = new ByteArrayOutputStream(1024);
        var channel = new OutputStreamBackedChannel(stream);
        var buffer = ByteBuffer.allocate(0).asReadOnlyBuffer();
        assertThrows(IllegalArgumentException.class, () -> channel.write(buffer));
    }

    @Test
    void open_and_close_and_try_to_write() throws Exception {
        var stream = new ByteArrayOutputStream(1024);
        var channel = new OutputStreamBackedChannel(stream);
        assertTrue(channel.isOpen());
        channel.close();
        assertThrows(ClosedChannelException.class, () -> channel.write(ByteBuffer.wrap(TEST_TEXT.getBytes())));
    }

}