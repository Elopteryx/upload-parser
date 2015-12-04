package com.github.elopteryx.upload.internal.integration;

import static org.junit.Assert.assertTrue;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

public class UndertowIntegrationTest {

    private static Undertow server;

    /**
     * Sets up the test environment, generates data to upload, starts an
     * Undertow instance which will receive the client requests.
     * @throws Exception If an error occurred with the servlets
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(UndertowIntegrationTest.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("ROOT.war")
                .addServlets(
                        Servlets.servlet("AsyncUploadServlet", AsyncUploadServlet.class)
                                .addMapping("/async")
                                .setAsyncSupported(true),
                        Servlets.servlet("BlockingUploadServlet", BlockingUploadServlet.class)
                                .addMapping("/blocking")
                                .setAsyncSupported(false)
                );

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        PathHandler path = Handlers.path(Handlers.redirect("/")).addPrefixPath("/", manager.start());

        server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(path)
                .build();
        server.start();
    }

    @Test
    public void test_with_a_real_request_simple_async() throws IOException {
        performRequest("http://localhost:8080/async?" + Constants.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_simple_blocking() throws IOException {
        performRequest("http://localhost:8080/blocking?" + Constants.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + Constants.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_io_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + Constants.IO_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_servlet_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + Constants.SERVLET_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_error_blocking() throws IOException {
        performRequest("http://localhost:8080/blocking?" + Constants.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_complex() throws IOException {
        performRequest("http://localhost:8080/async?" + Constants.COMPLEX, HttpServletResponse.SC_OK);
    }

    private void performRequest(String url, int expectedStatus) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(url);

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("filefield1", Constants.largeFile, ContentType.create("application/octet-stream"), "file1.txt")
                    .addBinaryBody("filefield2", Constants.emptyFile, ContentType.create("text/plain"), "file2.txt")
                    .addBinaryBody("filefield3", Constants.smallFile, ContentType.create("application/octet-stream"), "file3.txt")
                    .addTextBody("textfield1", Constants.textValue1)
                    .addTextBody("textfield2", Constants.textValue2)
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

    @AfterClass
    public static void tearDown() {
        server.stop();
    }
}
