package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.PartStream;
import com.elopteryx.paint.upload.UploadParser;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AsyncUploadParserTest {

    private ByteArrayInputStream emptyFile;
    private ByteArrayInputStream smallFile;
    private ByteArrayInputStream largeFile;
    
    private Undertow server;
    
    @Before
    public void setUp() throws ServletException {
        emptyFile = new ByteArrayInputStream(new byte[0]);
        smallFile = new ByteArrayInputStream("0123456789".getBytes(StandardCharsets.UTF_8));
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < 10000; i++)
            builder.append(random.nextInt(100));
        largeFile = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(getClass().getClassLoader())
                .setContextPath("/")
                .setDeploymentName("ROOT.war")
                .addServlets(
                        Servlets.servlet("FileUploadServlet", TestFileUploadServlet.class)
                                .addMapping("/")
                                .setAsyncSupported(true));

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
    public void test_with_a_real_request() throws IOException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost("http://localhost:8080" + "/FileUpload");

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("filefield1", largeFile, ContentType.create("application/octet-stream"), "file1.txt")
                    .addBinaryBody("filefield2", emptyFile, ContentType.create("text/plain"), "file2.txt")
                    .addBinaryBody("filefield3", smallFile, ContentType.create("application/octet-stream"), "file3.txt")
                    .addTextBody("textfield1", "íéáűúőóüö")
                    .addTextBody("textfield2", "abcdef")
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .build();
            
            httppost.setEntity(entity);
            System.out.println("executing request " + httppost.getRequestLine());
            try (CloseableHttpResponse response = httpClient.execute(httppost)) {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();
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


    @WebServlet(value = "/FileUploadServlet", asyncSupported = true)
    public static class TestFileUploadServlet extends HttpServlet {

        /**
         * Directory where uploaded files will be saved, its relative to
         * the web application directory.
         */
        private static final String UPLOAD_DIR = "uploads";

        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
                throws ServletException, IOException {


            // gets absolute path of the web application
//            String applicationPath = request.getServletContext().getRealPath("");
            // constructs path of the directory to save uploaded file
//            final Path uploadFilePath = Paths.get(applicationPath, UPLOAD_DIR);

//            if (!Files.isDirectory(uploadFilePath)) {
//                Files.createDirectories(uploadFilePath);
//            }
//            System.out.println("Upload File Directory=" + uploadFilePath.toAbsolutePath().toString());

            // Check that we have a file upload request
            if (!UploadParser.isMultipart(request))
                throw new ServletException("Not multipart!");

            final List<ByteArrayOutputStream> formFields = new ArrayList<>();

            UploadParser parser = UploadParser.newParser(request, response)
                    .onPartBegin((context, buffer) -> {
                        System.out.println("Start!");
                        //use the buffer to detect file type
                        PartStream part = context.getCurrentPart();
                        String name = part.getName();
                        if (part.isFile()) {
                            if ("".equals(part.getSubmittedFileName()))
                                throw new IOException("No file was chosen for the form field!");
                            System.out.println("File field " + name + " with file name "
                                    + part.getSubmittedFileName() + " detected!");
                            for (String header : part.getHeaderNames())
                                System.out.println(header + " " + part.getHeader(header));
                            part.getHeaders("content-type");
                            System.out.println(part.getContentType());
//                        Path path = uploadFilePath.resolve(part.getSubmittedFileName());
//                        return Channels.newChannel(Files.newOutputStream(path));
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            formFields.add(baos);
                            return Channels.newChannel(baos);
                        } else {
                            for (String header : part.getHeaderNames())
                                System.out.println(header + " " + part.getHeader(header));
                            System.out.println(part.getContentType());
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            formFields.add(baos);
                            return Channels.newChannel(baos);
                        }
                    })
                    .onPartEnd(context -> {
                        if(context.getCurrentChannel() != null)
                            context.getCurrentChannel().close();
                        System.out.println(context.getCurrentPart().getKnownSize());
                        System.out.println("Part success!");
                    })
                    .onRequestComplete(context -> {
                        System.out.println("Success!");
                        context.getResponse().setStatus(HttpServletResponse.SC_OK);
                        for (ByteArrayOutputStream baos : formFields)
                            System.out.println(baos.toString());
                        context.getRequest().getAsyncContext().complete();
                        context.getResponse().setStatus(200);
                    })
                    .onError((context, throwable) -> {
                        System.out.println("Error!");
                        throwable.printStackTrace();
                        for (ByteArrayOutputStream baos : formFields)
                            System.out.println(baos.toString());
                        try {
                            context.getResponse().sendError(500);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    })
                    .sizeThreshold(4096)
                    .maxPartSize(Long.MAX_VALUE)
                    .maxRequestSize(Long.MAX_VALUE);
            parser.setup();
        }
    }
}
