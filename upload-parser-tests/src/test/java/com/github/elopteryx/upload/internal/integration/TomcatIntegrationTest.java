package com.github.elopteryx.upload.internal.integration;

import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withOneLargerPicture;
import static com.github.elopteryx.upload.internal.integration.RequestSupplier.withOneSmallerPicture;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletResponse;

class TomcatIntegrationTest {

    private static Tomcat server;

    /**
     * Sets up the test environment, generates data to upload, starts a
     * Tomcat instance which will receive the client requests.
     * @throws Exception If an error occurred with the servlets
     */
    @BeforeAll
    static void setUpClass() throws Exception {
        server = new Tomcat();

        final var base = Paths.get("build/tomcat");
        Files.createDirectories(base);

        server.setPort(8100);
        server.setBaseDir("build/tomcat");
        server.getHost().setAppBase("build/tomcat");
        server.getHost().setAutoDeploy(true);
        server.getHost().setDeployOnStartup(true);

        final var context = (StandardContext) server.addWebapp("", base.toAbsolutePath().toString());

        final var additionWebInfClasses = Paths.get("build/classes");
        final WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                additionWebInfClasses.toAbsolutePath().toString(), "/"));
        context.setResources(resources);
        context.getJarScanner().setJarScanFilter((jarScanType, jarName) -> false);

        server.getConnector();
        server.start();
    }

    @Test
    void test_with_a_real_request_simple_async() {
        performRequest("http://localhost:8100/async?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    void test_with_a_real_request_simple_blocking() {
        performRequest("http://localhost:8100/blocking?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    void test_with_a_real_request_threshold_lesser_async() {
        performRequest("http://localhost:8100/async?" + ClientRequest.THRESHOLD_LESSER, HttpServletResponse.SC_OK, withOneSmallerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_lesser_blocking() {
        performRequest("http://localhost:8100/blocking?" + ClientRequest.THRESHOLD_LESSER, HttpServletResponse.SC_OK, withOneSmallerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_greater_async() {
        performRequest("http://localhost:8100/async?" + ClientRequest.THRESHOLD_GREATER, HttpServletResponse.SC_OK, withOneLargerPicture());
    }

    @Test
    void test_with_a_real_request_threshold_greater_blocking() {
        performRequest("http://localhost:8100/blocking?" + ClientRequest.THRESHOLD_GREATER, HttpServletResponse.SC_OK, withOneLargerPicture());
    }

    @Test
    void test_with_a_real_request_error_async() {
        performRequest("http://localhost:8100/async?" + ClientRequest.ERROR, null);
    }

    @Test
    void test_with_a_real_request_io_error_upon_error_async() {
        performRequest("http://localhost:8100/async?" + ClientRequest.IO_ERROR_UPON_ERROR, null);
    }

    @Test
    void test_with_a_real_request_servlet_error_upon_error_async() {
        performRequest("http://localhost:8100/async?" + ClientRequest.SERVLET_ERROR_UPON_ERROR, null);
    }

    @Test
    void test_with_a_real_request_error_blocking() {
        performRequest("http://localhost:8100/blocking?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void test_with_a_real_request_complex() {
        performRequest("http://localhost:8100/async?" + ClientRequest.COMPLEX, HttpServletResponse.SC_OK);
    }

    private void performRequest(final String url, final Integer expectedStatus) {
        try {
            ClientRequest.performRequest(url, expectedStatus);
        } catch (final IOException e) {
            if (expectedStatus != null) {
                fail("Status returned: " + expectedStatus);
            }
        }
    }

    private void performRequest(final String url, final Integer expectedStatus, final ByteBuffer requestData) {
        try {
            ClientRequest.performRequest(url, expectedStatus, requestData);
        } catch (final IOException e) {
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
