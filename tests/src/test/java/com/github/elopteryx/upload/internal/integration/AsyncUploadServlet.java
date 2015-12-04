package com.github.elopteryx.upload.internal.integration;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.PartStream;
import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.internal.NullChannel;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@WebServlet(value = "/async", asyncSupported = true)
public class AsyncUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String query = request.getQueryString();
        switch (query) {
            case ClientRequest.SIMPLE:
                simple(request, response);
                break;
            case ClientRequest.ERROR:
                error(request, response);
                break;
            case ClientRequest.IO_ERROR_UPON_ERROR:
                ioErrorUponError(request);
                break;
            case ClientRequest.SERVLET_ERROR_UPON_ERROR:
                servletErrorUponError(request);
                break;
            case ClientRequest.COMPLEX:
                complex(request, response);
                break;
            default:
                break;
        }

    }

    private void simple(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    if (context.getPartStreams().size() == 1) {
                        Path dir = ClientRequest.fileSystem.getPath("");
                        Path temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(Files.newByteChannel(temp, EnumSet.of(CREATE, TRUNCATE_EXISTING, WRITE)));
                    } else if (context.getPartStreams().size() == 2) {
                        Path dir = ClientRequest.fileSystem.getPath("");
                        Path temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(Files.newOutputStream(temp));
                    } else if (context.getPartStreams().size() == 3) {
                        Path dir = ClientRequest.fileSystem.getPath("");
                        Path temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(temp);
                    } else {
                        return PartOutput.from(new NullChannel());
                    }
                })
                .onPartEnd(context -> {
                    if (context.getCurrentOutput() != null && context.getCurrentOutput().safeToCast(Channel.class)) {
                        Channel channel = context.getCurrentOutput().unwrap(Channel.class);
                        if (channel.isOpen()) {
                            // The parser should close it
                            fail();
                        }
                    }
                })
                .onRequestComplete(context -> {
                    request.getAsyncContext().complete();
                    response.setStatus(200);
                })
                .setupAsyncParse(request);
    }

    private class EvilOutput extends PartOutput {
        EvilOutput(Object value) {
            super(value);
        }
    }

    private void error(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> new EvilOutput("This will cause an error!"))
                .onError((context, throwable) -> response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
                .setupAsyncParse(request);
    }

    private void ioErrorUponError(HttpServletRequest request) throws ServletException, IOException {

        UploadParser.newParser()
                .onRequestComplete(context -> {
                    throw new IOException();
                })
                .onError((context, throwable) -> {
                    throw new ServletException();
                })
                .setupAsyncParse(request);
    }

    private void servletErrorUponError(HttpServletRequest request) throws ServletException, IOException {

        // onError will not be called for ServletException!
        UploadParser.newParser()
                .onRequestComplete(context -> {
                    throw new ServletException();
                })
                .setupAsyncParse(request);
    }

    private void complex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (!UploadParser.isMultipart(request)) {
            throw new ServletException("Not multipart!");
        }

        List<ByteArrayOutputStream> formFields = new ArrayList<>();

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    System.out.println("Start!");
                    //use the buffer to detect file type
                    String contentType = ClientRequest.tika.detect(buffer.array());
                    assertNotNull(contentType);
                    System.out.println(contentType);
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
                })
                .onPartEnd(context -> {
                    System.out.println(context.getCurrentPart().getKnownSize());
                    System.out.println("Part success!");
                })
                .onRequestComplete(context -> {
                    System.out.println("Request complete!");
                    System.out.println("Total parts: " + context.getPartStreams().size());

                    assertTrue(Arrays.equals(formFields.get(0).toByteArray(), ClientRequest.largeFile));
                    assertTrue(Arrays.equals(formFields.get(1).toByteArray(), ClientRequest.emptyFile));
                    assertTrue(Arrays.equals(formFields.get(2).toByteArray(), ClientRequest.smallFile));
                    assertTrue(Arrays.equals(formFields.get(3).toByteArray(), ClientRequest.textValue1.getBytes(ISO_8859_1)));
                    assertTrue(Arrays.equals(formFields.get(4).toByteArray(), ClientRequest.textValue2.getBytes(ISO_8859_1)));

                    context.getUserObject(HttpServletResponse.class).setStatus(HttpServletResponse.SC_OK);

                    for (ByteArrayOutputStream baos : formFields) {
                        System.out.println(baos.toString());
                    }
                    context.getRequest().getAsyncContext().complete();
                })
                .onError((context, throwable) -> {
                    System.out.println("Error!");
                    throwable.printStackTrace();
                    for (ByteArrayOutputStream baos : formFields) {
                        System.out.println(baos.toString());
                    }
                    response.sendError(500);
                })
                .userObject(response)
                .sizeThreshold(4096)
                .maxPartSize(Long.MAX_VALUE)
                .maxRequestSize(Long.MAX_VALUE)
                .setupAsyncParse(request);
    }
}
