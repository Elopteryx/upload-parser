package com.github.elopteryx.upload.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

public class NullChannelTest {

    @Test
    public void read_from_open_channel() throws Exception {
        NullChannel channel = new NullChannel();
        assertEquals(-1, channel.read(ByteBuffer.allocate(0)));
    }

    @Test(expected = ClosedChannelException.class)
    public void read_from_closed_channel() throws Exception {
        NullChannel channel = new NullChannel();
        assertTrue(channel.isOpen());
        channel.close();
        assertFalse(channel.isOpen());
        channel.read(ByteBuffer.allocate(0));
    }

    @Test
    public void write_to_open_channel() throws Exception {
        NullChannel channel = new NullChannel();
        channel.write(ByteBuffer.allocate(0));
    }

    @Test(expected = ClosedChannelException.class)
    public void write_to_closed_channel() throws Exception {
        NullChannel channel = new NullChannel();
        assertTrue(channel.isOpen());
        channel.close();
        assertFalse(channel.isOpen());
        channel.write(ByteBuffer.allocate(0));
    }

}