package com.github.elopteryx.upload.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class ByteBufferBackedOutputStreamTest {

    private static final String TEST_TEXT = "Test text.";

    @Test(expected = IllegalArgumentException.class)
    public void create_with_direct() throws Exception {
        new ByteBufferBackedOutputStream(ByteBuffer.allocate(0).asReadOnlyBuffer());
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_with_read_only() throws Exception {
        new ByteBufferBackedOutputStream(ByteBuffer.allocateDirect(0));
    }

    @Test
    public void write_a_byte() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteBufferBackedOutputStream stream = new ByteBufferBackedOutputStream(buffer);
        byte oneByte = "T".getBytes()[0];
        stream.write(oneByte);
        assertEquals(buffer.get(0), oneByte);
    }

    @Test(expected = BufferOverflowException.class)
    public void try_to_write_an_exhausted_stream() throws Exception {
        ByteBufferBackedOutputStream stream = new ByteBufferBackedOutputStream(ByteBuffer.allocate(0));
        stream.write(1);
    }

    @Test(expected = IOException.class)
    public void try_to_write_a_closed_stream() throws Exception {
        ByteBufferBackedOutputStream stream = new ByteBufferBackedOutputStream(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        stream.close();
        stream.write(1);
    }

    @Test
    public void write_into_byte_array() throws Exception {
        byte[] bytes = TEST_TEXT.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteBufferBackedOutputStream stream = new ByteBufferBackedOutputStream(buffer);
        stream.write(bytes);
        assertEquals(TEST_TEXT, new String(buffer.array(), 0, bytes.length));
    }

    @Test(expected = BufferOverflowException.class)
    public void try_to_read_an_exhausted_stream_to_array() throws Exception {
        ByteBufferBackedOutputStream stream = new ByteBufferBackedOutputStream(ByteBuffer.allocate(0));
        stream.write(TEST_TEXT.getBytes());
    }

    @Test(expected = IOException.class)
    public void try_to_read_a_closed_stream_to_array() throws Exception {
        ByteBufferBackedOutputStream stream = new ByteBufferBackedOutputStream(ByteBuffer.allocate(1024));
        stream.close();
        stream.write(TEST_TEXT.getBytes());
    }

}