package com.github.elopteryx.upload.internal.integration;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.jimfs.Jimfs;
import org.apache.tika.Tika;

import java.nio.file.FileSystem;
import java.util.Random;

public class Constants {

    public static final String SIMPLE = "simple";
    public static final String ERROR = "error";
    public static final String IO_ERROR_UPON_ERROR = "io_error_upon_error";
    public static final String SERVLET_ERROR_UPON_ERROR = "servlet_error_upon_error";
    public static final String COMPLEX = "complex";

    public static final FileSystem fileSystem = Jimfs.newFileSystem();

    public static final Tika tika = new Tika();

    public static final byte[] emptyFile;
    public static final byte[] smallFile;
    public static final byte[] largeFile;

    public static final String textValue1 = "íéáűúőóüö";
    public static final String textValue2 = "abcdef";

    static {
        emptyFile = new byte[0];
        smallFile = "0123456789".getBytes(UTF_8);
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            builder.append(random.nextInt(100));
        }
        largeFile = builder.toString().getBytes(UTF_8);
    }
}
