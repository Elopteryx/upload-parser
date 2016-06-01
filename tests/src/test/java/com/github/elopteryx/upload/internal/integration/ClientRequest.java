package com.github.elopteryx.upload.internal.integration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;

import com.google.common.jimfs.Jimfs;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.tika.Tika;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Utility class for making multipart requests.
 */
public final class ClientRequest {

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
        for (int i = 0; i < 100000; i++) {
            builder.append(random.nextInt(100));
        }
        largeFile = builder.toString().getBytes(UTF_8);
    }

    /**
     * Creates and sends a randomized multipart request for the
     * given address.
     * @param url The target address
     * @param expectedStatus The expected HTTP response, can be null
     * @throws IOException If an IO error occurred
     */
    public static void performRequest(String url, Integer expectedStatus) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(url);

            HttpEntity entity = MultipartEntityBuilder.create()
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

            httppost.setEntity(entity);
            System.out.println("executing request " + httppost.getRequestLine());
            try (CloseableHttpResponse response = httpClient.execute(httppost)) {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();
                assertTrue(response.getStatusLine().getStatusCode() == expectedStatus);
                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                }
                EntityUtils.consume(resEntity);
            }
        }
    }

    private static byte[] getContents(String resource) {
        try {
            Path path = Paths.get(ClientRequest.class.getResource(resource).toURI());
            return Files.readAllBytes(path);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
