package com.elopteryx.paint.upload.impl;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertTrue;

import com.elopteryx.paint.upload.OnError;
import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.OnRequestComplete;
import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.PartStream;
import com.elopteryx.paint.upload.UploadParser;
import com.elopteryx.paint.upload.UploadContext;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IntegrationTest {

    private static byte[] emptyFile;
    private static byte[] smallFile;
    private static byte[] largeFile;

    private static final String textValue1 = "íéáűúőóüö";
    private static final String textValue2 = "abcdef";
    
    private static Undertow server;

    private static FileSystem fileSystem;

    /**
     * Sets up the test environment, generates data to upload, starts an
     * Undertow instance which will receive the client requests.
     * @throws ServletException If an error occurred with the servlets
     */
    @BeforeClass
    public static void setUpClass() throws ServletException {
        emptyFile = new byte[0];
        smallFile = "0123456789".getBytes(UTF_8);
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            builder.append(random.nextInt(100));
        }
        largeFile = builder.toString().getBytes(UTF_8);

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(IntegrationTest.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("ROOT.war")
                .addServlets(
                        Servlets.servlet("AsyncSimpleUploadServlet", AsyncSimpleUploadServlet.class)
                                .addMapping("/AsyncSimple")
                                .setAsyncSupported(true),
                        Servlets.servlet("BlockingSimpleUploadServlet", BlockingSimpleUploadServlet.class)
                                .addMapping("/BlockingSimple")
                                .setAsyncSupported(true),
                        Servlets.servlet("AsyncErrorUploadServlet", AsyncErrorUploadServlet.class)
                                .addMapping("/AsyncError")
                                .setAsyncSupported(true),
                        Servlets.servlet("BlockingErrorUploadServlet", BlockingErrorUploadServlet.class)
                                .addMapping("/BlockingError")
                                .setAsyncSupported(true),
                        Servlets.servlet("AsyncComplexUploadServlet", AsyncComplexUploadServlet.class)
                                .addMapping("/AsyncComplex")
                                .setAsyncSupported(true)
                );

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        PathHandler path = Handlers.path(Handlers.redirect("/")).addPrefixPath("/", manager.start());

        server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(path)
                .build();
        server.start();

        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void test_with_a_real_request_simple_async() throws IOException {
        performRequest("http://localhost:8080" + "/AsyncSimple", HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_simple_blocking() throws IOException {
        performRequest("http://localhost:8080" + "/BlockingSimple", HttpServletResponse.SC_OK);
    }

    @Test
    public void test_with_a_real_request_error_async() throws IOException {
        performRequest("http://localhost:8080" + "/AsyncError", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_error_blocking() throws IOException {
        performRequest("http://localhost:8080" + "/BlockingError", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void test_with_a_real_request_complex() throws IOException {
        performRequest("http://localhost:8080" + "/AsyncComplex", HttpServletResponse.SC_OK);
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

    @AfterClass
    public static void tearDown() {
        server.stop();
    }


    @WebServlet(value = "/AsyncSimpleUploadServlet", asyncSupported = true)
    public static class AsyncSimpleUploadServlet extends HttpServlet {

        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
                throws ServletException, IOException {

            UploadParser.newParser()
                    .onPartBegin(new OnPartBegin() {
                        @Nonnull
                        @Override
                        public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
                            if (context.getPartStreams().size() == 1) {
                                Path dir = IntegrationTest.fileSystem.getPath("");
                                Path temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                                return PartOutput.from(Files.newByteChannel(temp, EnumSet.of(CREATE, TRUNCATE_EXISTING, WRITE)));
                            } else if (context.getPartStreams().size() == 2) {
                                Path dir = IntegrationTest.fileSystem.getPath("");
                                Path temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                                return PartOutput.from(Files.newOutputStream(temp));
                            } else if (context.getPartStreams().size() == 3) {
                                Path dir = IntegrationTest.fileSystem.getPath("");
                                Path temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                                return PartOutput.from(temp);
                            } else {
                                return PartOutput.from(new NullChannel());
                            }
                        }
                    })
                    .onPartEnd(new OnPartEnd() {
                        @Override
                        public void onPartEnd(UploadContext context) throws IOException {
                            if (context.getCurrentOutput().safeToCast(Channel.class)) {
                                Channel channel = context.getCurrentOutput().unwrap(Channel.class);
                                if (channel.isOpen()) {
                                    channel.close();
                                }
                            }
                        }
                    })
                    .onRequestComplete(new OnRequestComplete() {
                        @Override
                        public void onRequestComplete(UploadContext context) throws IOException, ServletException {
                            request.getAsyncContext().complete();
                            response.setStatus(200);
                        }
                    })
                    .setupAsyncParse(request);
        }
    }

    @WebServlet(value = "/BlockingSimpleUploadServlet", asyncSupported = true)
    public static class BlockingSimpleUploadServlet extends HttpServlet {

        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
                throws ServletException, IOException {

            UploadContext context = UploadParser.newParser()
                    .onPartEnd(new OnPartEnd() {
                        @Override
                        public void onPartEnd(UploadContext context) throws IOException {
                            Channel channel = context.getCurrentOutput().unwrap(Channel.class);
                            if (channel.isOpen()) {
                                channel.close();
                            }
                        }
                    })
                    .onRequestComplete(new OnRequestComplete() {
                        @Override
                        public void onRequestComplete(UploadContext context) throws IOException, ServletException {
                            response.setStatus(200);
                        }
                    })
                    .doBlockingParse(request);
            assertTrue(context.getPartStreams().size() == 5);
        }
    }

    @WebServlet(value = "/AsyncErrorUploadServlet", asyncSupported = true)
    public static class AsyncErrorUploadServlet extends HttpServlet {

        private class EvilOutput extends PartOutput {
            EvilOutput(Object value) {
                super(value);
            }
        }

        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
                throws ServletException, IOException {

            UploadParser.newParser()
                    .onPartBegin(new OnPartBegin() {
                        @Nonnull
                        @Override
                        public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
                            return new EvilOutput("This will cause an error!");
                        }
                    })
                    .onError(new OnError() {
                        @Override
                        public void onError(UploadContext context, Throwable throwable) throws IOException, ServletException {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                    })
                    .setupAsyncParse(request);
        }
    }

    @WebServlet(value = "/BlockingErrorUploadServlet", asyncSupported = true)
    public static class BlockingErrorUploadServlet extends HttpServlet {

        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
                throws ServletException, IOException {

            UploadParser.newParser()
                    .onError(new OnError() {
                        @Override
                        public void onError(UploadContext context, Throwable throwable) throws IOException, ServletException {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                    })
                    .maxRequestSize(4096)
                    .doBlockingParse(request);
        }
    }

    @WebServlet(value = "/AsyncComplexUploadServlet", asyncSupported = true)
    public static class AsyncComplexUploadServlet extends HttpServlet {

        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
                throws ServletException, IOException {

            if (!UploadParser.isMultipart(request)) {
                throw new ServletException("Not multipart!");
            }

            final List<ByteArrayOutputStream> formFields = new ArrayList<>();

            UploadParser.newParser()
                    .onPartBegin(new OnPartBegin() {
                        @Nonnull
                        @Override
                        public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
                            System.out.println("Start!");
                            //use the buffer to detect file type
                            PartStream part = context.getCurrentPart();
                            String name = part.getName();
                            if (part.isFile()) {
                                if ("".equals(part.getSubmittedFileName())) {
                                    throw new IOException("No file was chosen for the form field!");
                                }
                                System.out.println("File field " + name + " with file name "
                                        + part.getSubmittedFileName() + " detected!");
                                for (String header : part.getHeaderNames()) {
                                    System.out.println(header + " " + part.getHeader(header));
                                }
                                part.getHeaders("content-type");
                                System.out.println(part.getContentType());
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                formFields.add(baos);
                                return PartOutput.from(baos);
                            } else {
                                for (String header : part.getHeaderNames()) {
                                    System.out.println(header + " " + part.getHeader(header));
                                }
                                System.out.println(part.getContentType());
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                formFields.add(baos);
                                return PartOutput.from(baos);
                            }
                        }
                    })
                    .onPartEnd(new OnPartEnd() {
                        @Override
                        public void onPartEnd(UploadContext context) throws IOException {
                            System.out.println(context.getCurrentPart().getKnownSize());
                            System.out.println("Part success!");
                        }
                    })
                    .onRequestComplete(new OnRequestComplete() {
                        @Override
                        public void onRequestComplete(UploadContext context) throws IOException, ServletException {
                            System.out.println("Request complete!");
                            System.out.println("Total parts: " + context.getPartStreams().size());

                            assertTrue(Arrays.equals(formFields.get(0).toByteArray(), largeFile));
                            assertTrue(Arrays.equals(formFields.get(1).toByteArray(), emptyFile));
                            assertTrue(Arrays.equals(formFields.get(2).toByteArray(), smallFile));
                            assertTrue(Arrays.equals(formFields.get(3).toByteArray(), textValue1.getBytes(ISO_8859_1)));
                            assertTrue(Arrays.equals(formFields.get(4).toByteArray(), textValue2.getBytes(ISO_8859_1)));

                            context.getUserObject(HttpServletResponse.class).setStatus(HttpServletResponse.SC_OK);

                            for (ByteArrayOutputStream baos : formFields) {
                                System.out.println(baos.toString());
                            }
                            context.getRequest().getAsyncContext().complete();
                        }
                    })
                    .onError(new OnError() {
                        @Override
                        public void onError(UploadContext context, Throwable throwable) throws IOException, ServletException {
                            System.out.println("Error!");
                            throwable.printStackTrace();
                            for (ByteArrayOutputStream baos : formFields) {
                                System.out.println(baos.toString());
                            }
                            response.sendError(500);
                        }
                    })
                    .userObject(response)
                    .sizeThreshold(4096)
                    .maxPartSize(Long.MAX_VALUE)
                    .maxRequestSize(Long.MAX_VALUE)
                    .setupAsyncParse(request);
        }
    }
}
