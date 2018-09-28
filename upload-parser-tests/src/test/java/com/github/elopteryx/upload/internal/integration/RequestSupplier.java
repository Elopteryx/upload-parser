package com.github.elopteryx.upload.internal.integration;

import static com.github.elopteryx.upload.internal.integration.ClientRequest.BOUNDARY;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Utility class which is responsible for the request body creation.
 */
final class RequestSupplier {

    static final byte[] EMPTY_FILE;
    static final byte[] SMALL_FILE;
    static final byte[] LARGE_FILE;

    static final String TEXT_VALUE_1 = "íéáűúőóüö";
    static final String TEXT_VALUE_2 = "abcdef";

    static {
        EMPTY_FILE = new byte[0];
        SMALL_FILE = "0123456789".getBytes(UTF_8);
        final var random = new Random();
        final var builder = new StringBuilder();
        for (var i = 0; i < 100000; i++) {
            builder.append(random.nextInt(100));
        }
        LARGE_FILE = builder.toString().getBytes(UTF_8);
    }

    static ByteBuffer withSeveralFields() {
        return RequestBuilder.newBuilder(BOUNDARY)
                .addFilePart("filefield1", LARGE_FILE, "application/octet-stream", "file1.txt")
                .addFilePart("filefield2", EMPTY_FILE, "text/plain", "file2.txt")
                .addFilePart("filefield3", SMALL_FILE, "application/octet-stream", "file3.txt")
                .addFormField("textfield1", TEXT_VALUE_1)
                .addFormField("textfield2", TEXT_VALUE_2)
                .addFilePart("filefield4", getContents("test.xlsx"), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test.xlsx")
                .addFilePart("filefield5", getContents("test.docx"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "test.docx")
                .addFilePart("filefield6", getContents("test.jpg"), "image/jpeg", "test.jpg")
                .finish();
    }

    static ByteBuffer withOneSmallerPicture() {
        return RequestBuilder.newBuilder(BOUNDARY)
                .addFilePart("filefield", new byte[512], "image/jpeg", "test.jpg")
                .finish();
    }

    static ByteBuffer withOneLargerPicture() {
        return RequestBuilder.newBuilder(BOUNDARY)
                .addFilePart("filefield", new byte[2048], "image/jpeg", "test.jpg")
                .finish();
    }

    private static byte[] getContents(final String resource) {
        try {
            final var path = Paths.get(ClientRequest.class.getResource(resource).toURI());
            return Files.readAllBytes(path);
        } catch (final URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
