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

    static final byte[] emptyFile;
    static final byte[] smallFile;
    static final byte[] largeFile;

    static final String textValue1 = "íéáűúőóüö";
    static final String textValue2 = "abcdef";

    static {
        emptyFile = new byte[0];
        smallFile = "0123456789".getBytes(UTF_8);
        var random = new Random();
        var builder = new StringBuilder();
        for (var i = 0; i < 100000; i++) {
            builder.append(random.nextInt(100));
        }
        largeFile = builder.toString().getBytes(UTF_8);
    }

    static ByteBuffer withSeveralFields() {
        return RequestBuilder.newBuilder(BOUNDARY)
                .addFilePart("filefield1", largeFile, "application/octet-stream", "file1.txt")
                .addFilePart("filefield2", emptyFile, "text/plain", "file2.txt")
                .addFilePart("filefield3", smallFile, "application/octet-stream", "file3.txt")
                .addFormField("textfield1", textValue1)
                .addFormField("textfield2", textValue2)
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

    private static byte[] getContents(String resource) {
        try {
            final var path = Paths.get(ClientRequest.class.getResource(resource).toURI());
            return Files.readAllBytes(path);
        } catch (final URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
