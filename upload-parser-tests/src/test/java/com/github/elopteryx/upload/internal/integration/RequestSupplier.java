package com.github.elopteryx.upload.internal.integration;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.function.Supplier;

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

    static Supplier<HttpEntity> withSeveralFields() {
        final var entity = MultipartEntityBuilder.create()
                .addBinaryBody("filefield1", largeFile, ContentType.create("application/octet-stream"), "file1.txt")
                .addBinaryBody("filefield2", emptyFile, ContentType.create("text/plain"), "file2.txt")
                .addBinaryBody("filefield3", smallFile, ContentType.create("application/octet-stream"), "file3.txt")
                .addTextBody("textfield1", textValue1)
                .addTextBody("textfield2", textValue2)
                .addBinaryBody("filefield4", getContents("test.xlsx"), ContentType.create("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), "test.xlsx")
                .addBinaryBody("filefield5", getContents("test.docx"), ContentType.create("application/vnd.openxmlformats-officedocument.wordprocessingml.document"), "test.docx")
                .addBinaryBody("filefield6", getContents("test.jpg"), ContentType.create("image/jpeg"), "test.jpg")
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();
        return () -> entity;
    }

    static Supplier<HttpEntity> withOneSmallerPicture() {
        final var entity = MultipartEntityBuilder.create()
                .addBinaryBody("filefield", new byte[512], ContentType.create("image/jpeg"), "test.jpg")
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();
        return () -> entity;
    }

    static Supplier<HttpEntity> withOneLargerPicture() {
        final var entity = MultipartEntityBuilder.create()
                .addBinaryBody("filefield", new byte[2048], ContentType.create("image/jpeg"), "test.jpg")
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();
        return () -> entity;
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
