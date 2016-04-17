package com.github.elopteryx.upload.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

public class OutputStreamBackedChannelTest {

    private static final String TEST_TEXT = "Test text.";

    @Test
    public void write_the_string() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
        OutputStreamBackedChannel channel = new OutputStreamBackedChannel(stream);
        ByteBuffer buffer = ByteBuffer.wrap(TEST_TEXT.getBytes());
        channel.write(buffer);
        assertEquals(TEST_TEXT, new String(buffer.array(), 0, buffer.position()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void try_to_write_from_direct_buffer() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
        OutputStreamBackedChannel channel = new OutputStreamBackedChannel(stream);
        ByteBuffer buffer = ByteBuffer.allocateDirect(0);
        channel.write(buffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void try_to_write_from_read_only_buffer() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
        OutputStreamBackedChannel channel = new OutputStreamBackedChannel(stream);
        ByteBuffer buffer = ByteBuffer.allocate(0).asReadOnlyBuffer();
        channel.write(buffer);
    }

    @Test(expected = ClosedChannelException.class)
    public void open_and_close_and_try_to_write() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
        OutputStreamBackedChannel channel = new OutputStreamBackedChannel(stream);
        assertTrue(channel.isOpen());
        channel.close();
        channel.write(ByteBuffer.wrap(TEST_TEXT.getBytes()));
    }

}