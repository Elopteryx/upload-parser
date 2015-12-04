package com.github.elopteryx.upload.internal.integration;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
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
        performRequest("http://localhost:8080/async?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_simple_blocking() throws IOException {
        performRequest("http://localhost:8080/blocking?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_io_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.IO_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_servlet_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.SERVLET_ERROR_UPON_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_error_blocking() throws IOException {
        performRequest("http://localhost:8080/blocking?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_complex() throws IOException {
        performRequest("http://localhost:8080/async?" + ClientRequest.COMPLEX, HttpServletResponse.SC_OK);
    }

    private void performRequest(String url, int expectedStatus) throws IOException {
        ClientRequest.performRequest(url, expectedStatus);
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }
}
