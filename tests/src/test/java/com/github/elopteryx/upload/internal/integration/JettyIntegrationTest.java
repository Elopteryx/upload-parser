package com.github.elopteryx.upload.internal.integration;

import static org.junit.Assert.fail;

import org.apache.http.ConnectionClosedException;
import org.apache.http.NoHttpResponseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketException;

public class JettyIntegrationTest {

    private static Server server;

    /**
     * Sets up the test environment, generates data to upload, starts a
     * Jetty instance which will receive the client requests.
     * @throws Exception If an error occurred with the servlets
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        server = new Server(8090);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(AsyncUploadServlet.class, "/async");
        handler.addServletWithMapping(BlockingUploadServlet.class, "/blocking");

        server.start();
    }

    @Test
    public void test_with_a_real_request_simple_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_simple_blocking() throws IOException {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_error_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_io_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.IO_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_servlet_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.SERVLET_ERROR_UPON_ERROR, null);
    }

    @Test
    public void test_with_a_real_request_error_blocking() throws IOException {
        performRequest("http://localhost:8090/blocking?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_complex() throws IOException {
        performRequest("http://localhost:8090/async?" + ClientRequest.COMPLEX, HttpServletResponse.SC_OK);
    }

    private void performRequest(String url, Integer expectedStatus) throws IOException {
        try {
            ClientRequest.performRequest(url, expectedStatus);
        } catch (NoHttpResponseException | SocketException | ConnectionClosedException e) {
            e.printStackTrace();
            if (expectedStatus != null) {
                fail();
            }
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }
}
