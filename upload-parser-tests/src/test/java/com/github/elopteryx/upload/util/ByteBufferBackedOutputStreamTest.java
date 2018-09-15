package com.github.elopteryx.upload.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

class ByteBufferBackedOutputStreamTest {

    private static final String TEST_TEXT = "Test text.";

    @Test
    void create_with_direct() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ByteBufferBackedOutputStream(ByteBuffer.allocate(0).asReadOnlyBuffer());
        });
    }

    @Test
    void create_with_read_only() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ByteBufferBackedOutputStream(ByteBuffer.allocateDirect(0));
        });
    }

    @Test
    void write_a_byte() throws Exception {
        var buffer = ByteBuffer.allocate(1024);
        var stream = new ByteBufferBackedOutputStream(buffer);
        var oneByte = "T".getBytes()[0];
        stream.write(oneByte);
        assertEquals(buffer.get(0), oneByte);
    }

    @Test
    void try_to_write_an_exhausted_stream() {
        var stream = new ByteBufferBackedOutputStream(ByteBuffer.allocate(0));
        assertThrows(BufferOverflowException.class, () -> stream.write(1));
    }

    @Test
    void try_to_write_a_closed_stream() throws Exception {
        var stream = new ByteBufferBackedOutputStream(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        stream.close();
        assertThrows(IOException.class, () -> stream.write(1));
    }

    @Test
    void write_into_byte_array() throws IOException {
        var bytes = TEST_TEXT.getBytes();
        var buffer = ByteBuffer.allocate(1024);
        var stream = new ByteBufferBackedOutputStream(buffer);
        stream.write(bytes);
        assertEquals(TEST_TEXT, new String(buffer.array(), 0, bytes.length));
    }

    @Test
    void try_to_read_an_exhausted_stream_to_array() {
        var stream = new ByteBufferBackedOutputStream(ByteBuffer.allocate(0));
        assertThrows(BufferOverflowException.class, () -> stream.write(TEST_TEXT.getBytes()));
    }

    @Test
    void try_to_read_a_closed_stream_to_array() throws Exception {
        var stream = new ByteBufferBackedOutputStream(ByteBuffer.allocate(1024));
        stream.close();
        assertThrows(IOException.class, () -> stream.write(TEST_TEXT.getBytes()));
    }

}