package com.elopteryx.paint.upload;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PartOutputTest {

    @Test
    public void create_channel_output() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel(byteArrayOutputStream);
        PartOutput output = PartOutput.from(channel);

        assertTrue(output.safeToCast(Channel.class));
        assertTrue(output.safeToCast(WritableByteChannel.class));
        assertFalse(output.safeToCast(OutputStream.class));
        assertFalse(output.safeToCast(ByteArrayOutputStream.class));

        assertNotNull(output.unwrap(WritableByteChannel.class));
    }

    @Test
    public void create_stream_output() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PartOutput output = PartOutput.from(byteArrayOutputStream);

        assertTrue(output.safeToCast(OutputStream.class));
        assertTrue(output.safeToCast(ByteArrayOutputStream.class));
        assertFalse(output.safeToCast(Channel.class));
        assertFalse(output.safeToCast(WritableByteChannel.class));

        assertNotNull(output.unwrap(OutputStream.class));
    }

    @Test
    public void create_path_output() throws IOException {
        Path path = Paths.get("");
        PartOutput output = PartOutput.from(path);

        assertTrue(output.safeToCast(Path.class));

        assertNotNull(output.unwrap(Path.class));
    }
}
