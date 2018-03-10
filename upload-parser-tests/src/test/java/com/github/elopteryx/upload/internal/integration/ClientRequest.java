package com.github.elopteryx.upload.internal.integration;

import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withSeveralFields;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.jimfs.Jimfs;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.function.Supplier;

/**
 * Utility class for making multipart requests.
 */
public final class ClientRequest {

    static final String SIMPLE = "simple";
    static final String THRESHOLD_LESSER = "threshold_lesser";
    static final String THRESHOLD_GREATER = "threshold_greater";
    static final String ERROR = "error";
    static final String IO_ERROR_UPON_ERROR = "io_error_upon_error";
    static final String SERVLET_ERROR_UPON_ERROR = "servlet_error_upon_error";
    static final String COMPLEX = "complex";

    static final FileSystem fileSystem = Jimfs.newFileSystem();

    static final Tika tika = new Tika();

    /**
     * Creates and sends a randomized multipart request for the
     * given address.
     * @param url The target address
     * @param expectedStatus The expected HTTP response, can be null
     * @throws IOException If an IO error occurred
     */
    public static void performRequest(String url, Integer expectedStatus) throws IOException {
        performRequest(url, expectedStatus, withSeveralFields());
    }

    /**
     * Creates and sends a randomized multipart request for the
     * given address.
     * @param url The target address
     * @param expectedStatus The expected HTTP response, can be null
     * @param requestSupplier Provides a multipart body, can't be null
     * @throws IOException If an IO error occurred
     */
    public static void performRequest(String url, Integer expectedStatus, Supplier<HttpEntity> requestSupplier) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(url);
            HttpEntity entity = requestSupplier.get();
            httppost.setEntity(entity);
            System.out.println("executing request " + httppost.getRequestLine());
            try (CloseableHttpResponse response = httpClient.execute(httppost)) {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();
                EntityUtils.consume(resEntity);
                if (expectedStatus != null) {
                    assertEquals(response.getStatusLine().getStatusCode(), (int) expectedStatus);
                }
                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                }

            }
        }
    }
}
