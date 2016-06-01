package com.github.elopteryx.upload.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

public class InputStreamBackedChannelTest {

    private static final String TEST_TEXT = "Test text.";

    @Test
    public void read_the_string() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_TEXT.getBytes());
        InputStreamBackedChannel channel = new InputStreamBackedChannel(stream);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
        assertEquals(TEST_TEXT, new String(buffer.array(), 0, buffer.position()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void try_to_read_to_direct_buffer() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_TEXT.getBytes());
        InputStreamBackedChannel channel = new InputStreamBackedChannel(stream);
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        channel.read(buffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void try_to_read_to_read_only_buffer() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_TEXT.getBytes());
        InputStreamBackedChannel channel = new InputStreamBackedChannel(stream);
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024).asReadOnlyBuffer();
        channel.read(buffer);
    }

    @Test(expected = ClosedChannelException.class)
    public void open_and_close_and_try_to_read() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_TEXT.getBytes());
        InputStreamBackedChannel channel = new InputStreamBackedChannel(stream);
        assertTrue(channel.isOpen());
        channel.close();
        channel.read(ByteBuffer.allocate(0));
    }

}