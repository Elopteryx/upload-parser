package com.github.elopteryx.upload.internal.integration;

import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withOneLargerPicture;
import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withOneSmallerPicture;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
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

        var handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(AsyncUploadServlet.class, "/async");
        handler.addServletWithMapping(BlockingUploadServlet.class, "/blocking");

        server.start();
    }

    @Test
    void test_with_a_real_request_simple_async() {
        performRequest("http://localhost:8090/async?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    void test_with_a_real_request_simple_blocking() {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    void test_with_a_real_request_threshold_lesser_async() {
        performRequest("http://localhost:8090/async?" + ClientRequest.THRESHOLD_LESSER, HttpServletResponse.SC_OK, withOneSmallerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_lesser_blocking() {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.THRESHOLD_LESSER, HttpServletResponse.SC_OK, withOneSmallerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_greater_async() {
        performRequest("http://localhost:8090/async?" + ClientRequest.THRESHOLD_GREATER, HttpServletResponse.SC_OK, withOneLargerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_greater_blocking() {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.THRESHOLD_GREATER, HttpServletResponse.SC_OK, withOneLargerPicture());
    }

    @Test
    void test_with_a_real_request_error_async() {
        performRequest("http://localhost:8090/async?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_io_error_upon_error_async() {
        performRequest("http://localhost:8090/async?" + ClientRequest.IO_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_servlet_error_upon_error_async() {
        performRequest("http://localhost:8090/async?" + ClientRequest.SERVLET_ERROR_UPON_ERROR, null);
    }

    @Test
    void test_with_a_real_request_error_blocking() {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_complex() {
        performRequest("http://localhost:8090/async?" + ClientRequest.COMPLEX, HttpServletResponse.SC_OK);
    }

    private void performRequest(String url, Integer expectedStatus) {
        try {
            ClientRequest.performRequest(url, expectedStatus);
        } catch (IOException e) {
            e.printStackTrace();
            if (expectedStatus != null) {
                fail("Status returned: " + expectedStatus);
            }
        }
    }

    private void performRequest(String url, Integer expectedStatus, ByteBuffer requestData) {
        try {
            ClientRequest.performRequest(url, expectedStatus, requestData);
        } catch (IOException e) {
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
