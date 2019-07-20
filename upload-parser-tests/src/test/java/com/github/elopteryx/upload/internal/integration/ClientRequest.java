package com.github.elopteryx.upload.internal.integration;

import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withSeveralFields;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.jimfs.Jimfs;
import org.apache.tika.Tika;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.time.Duration;

/**
 * Utility class for making multipart requests.
 */
public final class ClientRequest {

    static final String BOUNDARY = "--TNoK9riv6EjfMhxBzj22SKGnOaIhZlxhar";

    static final String SIMPLE = "simple";
    static final String THRESHOLD_LESSER = "threshold_lesser";
    static final String THRESHOLD_GREATER = "threshold_greater";
    static final String ERROR = "error";
    static final String IO_ERROR_UPON_ERROR = "io_error_upon_error";
    static final String SERVLET_ERROR_UPON_ERROR = "servlet_error_upon_error";
    static final String COMPLEX = "complex";

    static final FileSystem FILE_SYSTEM = Jimfs.newFileSystem();

    static final Tika TIKA = new Tika();

    /**
     * Creates and sends a randomized multipart request for the
     * given address.
     * @param url The target address
     * @param expectedStatus The expected HTTP response, can be null
     * @throws IOException If an IO error occurred
     */
    public static void performRequest(final String url, final Integer expectedStatus) throws IOException {
        performRequest(url, expectedStatus, withSeveralFields());
    }

    /**
     * Creates and sends a randomized multipart request for the
     * given address.
     * @param url The target address
     * @param expectedStatus The expected HTTP response, can be null
     * @param requestData The multipart body, can't be null
     * @throws IOException If an IO error occurred
     */
    public static void performRequest(final String url, final Integer expectedStatus, final ByteBuffer requestData) throws IOException {
        final var client = HttpClient.newBuilder().version(HTTP_1_1).build();
        final var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestData.array(), 0, requestData.limit()))
                .build();
        try {
            client.send(request, responseInfo -> {
                final var statusCode = responseInfo.statusCode();
                System.out.println("----------------------------------------");
                System.out.println(statusCode);
                if (expectedStatus != null) {
                    assertEquals((int) expectedStatus, statusCode);
                }
                return HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
            });
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
