package com.github.elopteryx.upload.internal.integration;

import static org.junit.Assert.fail;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.http.NoHttpResponseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TomcatIntegrationTest {

    private static Tomcat server;

    /**
     * Sets up the test environment, generates data to upload, starts a
     * Tomcat instance which will receive the client requests.
     * @throws Exception If an error occurred with the servlets
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        server = new Tomcat();

        Path base = Paths.get("build/tomcat");
        Files.createDirectories(base);

        server.setPort(8100);
        server.setBaseDir("build/tomcat");
        server.getHost().setAppBase("build/tomcat");
        server.getHost().setAutoDeploy(true);
        server.getHost().setDeployOnStartup(true);

        StandardContext context = (StandardContext) server.addWebapp("/", base.toAbsolutePath().toString());

        Path additionWebInfClasses = Paths.get("build/classes");
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                additionWebInfClasses.toAbsolutePath().toString(), "/"));
        context.setResources(resources);

        server.start();
    }

    @Test
    public void test_with_a_real_request_simple_async() throws IOException {
        performRequest("http://localhost:8100/async?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_simple_blocking() throws IOException {
        performRequest("http://localhost:8100/blocking?" + ClientRequest.SIMPLE, HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_error_async() throws IOException {
        performRequest("http://localhost:8100/async?" + ClientRequest.ERROR, null);
    }

    @Test
    public void test_with_a_real_request_io_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8100/async?" + ClientRequest.IO_ERROR_UPON_ERROR, null);
    }

    @Test
    public void test_with_a_real_request_servlet_error_upon_error_async() throws IOException {
        performRequest("http://localhost:8100/async?" + ClientRequest.SERVLET_ERROR_UPON_ERROR, null);
    }

    @Test
    public void test_with_a_real_request_error_blocking() throws IOException {
        performRequest("http://localhost:8100/blocking?" + ClientRequest.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_complex() throws IOException {
        performRequest("http://localhost:8100/async?" + ClientRequest.COMPLEX, HttpServletResponse.SC_OK);
    }

    private void performRequest(String url, Integer expectedStatus) throws IOException {
        try {
            ClientRequest.performRequest(url, expectedStatus);
        } catch (NoHttpResponseException e) {
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
