package com.elopteryx.paint.upload.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class ByteChannels {
    
    private static final int BUFFER_SIZE = 4096;

    public static boolean contentEquals(Path p1, Path p2) throws IOException {
        requireNonNull(p1);
        requireNonNull(p2);
        return Files.size(p1) == Files.size(p2) && contentEquals(Files.newByteChannel(p1), Files.newByteChannel(p2));
    }
    
    public static boolean contentEquals(ReadableByteChannel channel1, ReadableByteChannel channel2) throws IOException {
        requireNonNull(channel1);
        requireNonNull(channel2);
        
        ByteBuffer buf1 = ByteBuffer.allocate(BUFFER_SIZE);
        ByteBuffer buf2 = ByteBuffer.allocate(BUFFER_SIZE);

        try(ReadableByteChannel ch1 = channel1; ReadableByteChannel ch2 = channel2) {
            while (true) {
                int n1 = ch1.read(buf1);
                int n2 = ch2.read(buf2);

                if (n1 == -1 || n2 == -1) return n1 == n2;

                buf1.flip();
                buf2.flip();

                for (int i = 0; i < Math.min(n1, n2); i++)
                    if (buf1.get() != buf2.get())
                        return false;

                buf1.compact();
                buf2.compact();
            }
        }
    }
    
}
