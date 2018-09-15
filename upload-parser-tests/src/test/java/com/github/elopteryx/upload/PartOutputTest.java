package com.github.elopteryx.upload;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

class PartOutputTest {

    @Test
    void create_channel_output() {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var channel = Channels.newChannel(byteArrayOutputStream);
        var output = PartOutput.from(channel);

        assertTrue(output.safeToCast(Channel.class));
        assertTrue(output.safeToCast(WritableByteChannel.class));
        assertFalse(output.safeToCast(OutputStream.class));
        assertFalse(output.safeToCast(ByteArrayOutputStream.class));

        assertNotNull(output.unwrap(WritableByteChannel.class));
    }

    @Test
    void create_stream_output() {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var output = PartOutput.from(byteArrayOutputStream);

        assertTrue(output.safeToCast(OutputStream.class));
        assertTrue(output.safeToCast(ByteArrayOutputStream.class));
        assertFalse(output.safeToCast(Channel.class));
        assertFalse(output.safeToCast(WritableByteChannel.class));

        assertNotNull(output.unwrap(OutputStream.class));
    }

    @Test
    void create_path_output() {
        var path = Paths.get("");
        var output = PartOutput.from(path);

        assertTrue(output.safeToCast(Path.class));

        assertNotNull(output.unwrap(Path.class));
    }
}
