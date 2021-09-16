package com.github.elopteryx.upload.internal.integration;

import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withOneLargerPicture;
import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withOneSmallerPicture;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import jakarta.servlet.http.HttpServletResponse;

class UndertowIntegrationTest {

    private static Undertow server;

    /**
     * Sets up the test environment, generates data to upload, starts an
     * Undertow instance which will receive the client requests.
     * @throws Exception If an error occurred with the servlets
     */
    @BeforeAll
    static void setUpClass() throws Exception {
        final var servletBuilder = Servlets.deployment()
                .setClassLoader(Thread.currentThread().getContextClassLoader())
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

        final var manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        final var path = Handlers.path(Handlers.redirect("/")).addPrefixPath("/", manager.start());

        server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(path)
                .build();
        server.start();
    }

    @Test
    void test_with_a_real_request_simple_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    void test_with_a_real_request_simple_blocking() throws IOException {
        performRequest("http://localhost:8080/blocking?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    void test_with_a_real_request_threshold_lesser_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.THRESHOLD_LESSER, HttpServletResponse.SC_OK, withOneSmallerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_lesser_blocking() throws IOException {
        performRequest("http://localhost:8080/blocking?" + ClientRequest.THRESHOLD_LESSER, HttpServletResponse.SC_OK, withOneSmallerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_greater_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.THRESHOLD_GREATER, HttpServletResponse.SC_OK, withOneLargerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_greater_blocking() throws IOException {
        performRequest("http://localhost:8080/blocking?" + ClientRequest.THRESHOLD_GREATER, HttpServletResponse.SC_OK, withOneLargerPicture());
    }

    @Test
    void test_with_a_real_request_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_io_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.IO_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_servlet_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.SERVLET_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_error_blocking() throws IOException {
        performRequest("http://localhost:8080/blocking?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_complex() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.COMPLEX, HttpServletResponse.SC_OK);
    }

    private void performRequest(final String url, final int expectedStatus) throws IOException {
        ClientRequest.performRequest(url, expectedStatus);
    }

    private void performRequest(final String url, final int expectedStatus, final ByteBuffer requestData) throws IOException {
        ClientRequest.performRequest(url, expectedStatus, requestData);
    }

    @AfterAll
    static void tearDown() {
        server.stop();
    }
}
