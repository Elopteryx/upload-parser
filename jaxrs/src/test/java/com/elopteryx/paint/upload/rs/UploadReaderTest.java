package com.elopteryx.paint.upload.rs;

import static org.junit.Assert.assertTrue;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.elopteryx.paint.upload.rs.errors.PartSizeMapper;
import com.elopteryx.paint.upload.rs.errors.RequestSizeMapper;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class UploadReaderTest {

    private static byte[] emptyFile;
    private static byte[] smallFile;
    private static byte[] largeFile;

    private static final String textValue1 = "íéáűúőóüö";
    private static final String textValue2 = "abcdef";

    private Undertow server;

    /**
     * Sets up the test environment, generates data to upload, starts an
     * Undertow instance which will receive the client requests.
     * @throws ServletException If an error occurred with the servlets
     */
    @Before
    public void setUp() throws ServletException {
        emptyFile = new byte[0];
        smallFile = "0123456789".getBytes(UTF_8);
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            builder.append(random.nextInt(100));
        }
        largeFile = builder.toString().getBytes(UTF_8);

        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.getActualResourceClasses().add(AsyncUploadController.class);
        deployment.getActualProviderClasses().addAll(Arrays.asList(CustomUploadReader.class, PartSizeMapper.class, RequestSizeMapper.class));

        ServletInfo restEasyServlet = Servlets.servlet("RestEasyServlet", HttpServlet30Dispatcher.class)
                .setAsyncSupported(true)
                .setLoadOnStartup(1)
                .addMapping("/*");

        DeploymentInfo deploymentInfo = new DeploymentInfo()
                .setContextPath("ROOT")
                .addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
                .addServlet(restEasyServlet).setDeploymentName("RestEasyUndertow")
                .setClassLoader(ClassLoader.getSystemClassLoader());

        DeploymentManager deploymentManager = Servlets.defaultContainer().addDeployment(deploymentInfo);
        deploymentManager.deploy();

        PathHandler path = Handlers.path(Handlers.redirect("/")).addPrefixPath("/", deploymentManager.start());

        server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(path)
                .build();
        server.start();
    }

    @Test
    public void test_the_upload_parser_with_jax_rs() throws IOException {
        performRequest("http://localhost:8080" + "/upload" + "/uploadWithParser", HttpServletResponse.SC_OK);
    }

    @Test
    public void test_the_upload_reader_with_jax_rs() throws IOException {
        performRequest("http://localhost:8080" + "/upload" + "/uploadWithReader", HttpServletResponse.SC_OK);
    }

    @Test
    public void test_the_upload_reader_with_jax_rs_part_size_limited() throws IOException {
        performRequest("http://localhost:8080" + "/upload" + "/uploadWithReaderAndPartLimit", HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

    @Test
    public void test_the_upload_reader_with_jax_rs_request_size_limited() throws IOException {
        performRequest("http://localhost:8080" + "/upload" + "/uploadWithReaderAndRequestLimit", HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    }

    private void performRequest(String url, int expectedStatus) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(url);

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("filefield1", largeFile, ContentType.create("application/octet-stream"), "file1.txt")
                    .addBinaryBody("filefield2", emptyFile, ContentType.create("text/plain"), "file2.txt")
                    .addBinaryBody("filefield3", smallFile, ContentType.create("application/octet-stream"), "file3.txt")
                    .addTextBody("textfield1", textValue1)
                    .addTextBody("textfield2", textValue2)
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

    @After
    public void tearDown() {
        server.stop();
    }

}
