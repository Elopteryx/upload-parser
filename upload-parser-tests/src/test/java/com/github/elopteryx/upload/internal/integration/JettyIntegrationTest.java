package com.github.elopteryx.upload.internal.integration;

import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withOneLargerPicture;
import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withOneSmallerPicture;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketException;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletResponse;

class JettyIntegrationTest {

    private static Server server;

    /**
     * Sets up the test environment, generates data to upload, starts a
     * Jetty instance which will receive the client requests.
     * @throws Exception If an error occurred with the servlets
     */
    @BeforeAll
    static void setUpClass() throws Exception {
        server = new Server(8090);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(AsyncUploadServlet.class, "/async");
        handler.addServletWithMapping(BlockingUploadServlet.class, "/blocking");

        server.start();
    }

    @Test
    void test_with_a_real_request_simple_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    void test_with_a_real_request_simple_blocking() throws IOException {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    void test_with_a_real_request_threshold_lesser_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.THRESHOLD_LESSER, HttpServletResponse.SC_OK, withOneSmallerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_lesser_blocking() throws IOException {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.THRESHOLD_LESSER, HttpServletResponse.SC_OK, withOneSmallerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_greater_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.THRESHOLD_GREATER, HttpServletResponse.SC_OK, withOneLargerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_greater_blocking() throws IOException {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.THRESHOLD_GREATER, HttpServletResponse.SC_OK, withOneLargerPicture());
    }

    @Test
    void test_with_a_real_request_error_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_io_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.IO_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_servlet_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.SERVLET_ERROR_UPON_ERROR, null);
    }

    @Test
    void test_with_a_real_request_error_blocking() throws IOException {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_complex() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.COMPLEX, HttpServletResponse.SC_OK);
    }

    private void performRequest(String url, Integer expectedStatus) throws IOException {
        try {
            ClientRequest.performRequest(url, expectedStatus);
        } catch (NoHttpResponseException | SocketException | ConnectionClosedException e) {
            e.printStackTrace();
            if (expectedStatus != null) {
                fail("Status returned: " + expectedStatus);
            }
        }
    }

    private void performRequest(String url, Integer expectedStatus, Supplier<HttpEntity> requestSupplier) throws IOException {
        try {
            ClientRequest.performRequest(url, expectedStatus, requestSupplier);
        } catch (NoHttpResponseException | SocketException e) {
            e.printStackTrace();
            if (expectedStatus != null) {
                fail("Status returned: " + expectedStatus);
            }
        }
    }

    @AfterAll
    static void tearDown() throws Exception {
        server.stop();
    }
}
