package com.github.elopteryx.upload.rs;

import com.github.elopteryx.upload.internal.integration.ClientRequest;
import com.github.elopteryx.upload.rs.errors.PartSizeMapper;
import com.github.elopteryx.upload.rs.errors.RequestSizeMapper;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

class UploadReaderTest {

    private static Undertow server;

    /**
     * Sets up the test environment, generates data to upload, starts an
     * Undertow instance which will receive the client requests.
     * @throws ServletException If an error occurred with the servlets
     */
    @BeforeEach
    void setUp() throws ServletException {

        final var deployment = new ResteasyDeploymentImpl();
        deployment.getActualResourceClasses().addAll(Arrays.asList(AsyncUploadController.class, ReaderUploadController.class));
        deployment.getActualProviderClasses().addAll(Arrays.asList(UploadReader.class, PartSizeMapper.class, RequestSizeMapper.class));

        final var restEasyServlet = Servlets.servlet("RestEasyServlet", HttpServlet30Dispatcher.class)
                .setAsyncSupported(true)
                .setLoadOnStartup(1)
                .addMapping("/*");

        final var deploymentInfo = new DeploymentInfo()
                .setContextPath("ROOT")
                .addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
                .addServlet(restEasyServlet).setDeploymentName("RestEasyUndertow")
                .setClassLoader(ClassLoader.getSystemClassLoader());

        final var deploymentManager = Servlets.defaultContainer().addDeployment(deploymentInfo);
        deploymentManager.deploy();

        final var path = Handlers.path(Handlers.redirect("/")).addPrefixPath("/", deploymentManager.start());

        server = Undertow.builder()
                .addHttpListener(8110, "localhost")
                .setHandler(path)
                .build();
        server.start();
    }

    @Test
    void test_the_upload_parser_with_jax_rs() throws IOException {
        performRequest("http://localhost:8110" + "/upload" + "/uploadWithParser", HttpServletResponse.SC_OK);
    }

    @Test
    void test_the_upload_reader_with_jax_rs() throws IOException {
        performRequest("http://localhost:8110" + "/upload" + "/uploadWithReader", HttpServletResponse.SC_OK);
    }

    @Test
    void test_the_upload_reader_with_jax_rs_invalid_injection() throws IOException {
        performRequest("http://localhost:8110" + "/upload" + "/uploadWithInvalidParameters", HttpServletResponse.SC_OK);
    }

    @Test
    void test_the_upload_reader_with_jax_rs_part_size_limited() throws IOException {
        performRequest("http://localhost:8110" + "/upload" + "/uploadWithReaderAndPartLimit", HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

    @Test
    void test_the_upload_reader_with_jax_rs_request_size_limited() throws IOException {
        performRequest("http://localhost:8110" + "/upload" + "/uploadWithReaderAndRequestLimit", HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    }

    private void performRequest(final String url, final int expectedStatus) throws IOException {
        ClientRequest.performRequest(url, expectedStatus);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

}
