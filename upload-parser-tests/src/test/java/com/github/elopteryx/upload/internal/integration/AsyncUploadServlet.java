package com.github.elopteryx.upload.internal.integration;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.elopteryx.upload.OnRequestComplete;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.util.ByteBufferBackedInputStream;
import com.github.elopteryx.upload.util.NullChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(value = "/async", asyncSupported = true)
public class AsyncUploadServlet extends HttpServlet {

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        final var query = request.getQueryString();
        switch (query) {
            case ClientRequest.SIMPLE:
                simple(request, response);
                break;
            case ClientRequest.THRESHOLD_LESSER:
                thresholdLesser(request, response);
                break;
            case ClientRequest.THRESHOLD_GREATER:
                thresholdGreater(request, response);
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

    private void simple(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    if (context.getPartStreams().size() == 1) {
                        var dir = ClientRequest.fileSystem.getPath("");
                        var temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(Files.newByteChannel(temp, EnumSet.of(CREATE, TRUNCATE_EXISTING, WRITE)));
                    } else if (context.getPartStreams().size() == 2) {
                        var dir = ClientRequest.fileSystem.getPath("");
                        var temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(Files.newOutputStream(temp));
                    } else if (context.getPartStreams().size() == 3) {
                        var dir = ClientRequest.fileSystem.getPath("");
                        var temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(temp);
                    } else {
                        return PartOutput.from(new NullChannel());
                    }
                })
                .onPartEnd(context -> {
                    if (context.getCurrentOutput() != null && context.getCurrentOutput().safeToCast(Channel.class)) {
                        var channel = context.getCurrentOutput().unwrap(Channel.class);
                        if (channel.isOpen()) {
                            fail("The parser should close it!");
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
        EvilOutput(final Object value) {
            super(value);
        }
    }

    private void thresholdLesser(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    final var currentPart = context.getCurrentPart();
                    assertTrue(currentPart.isFinished());
                    return PartOutput.from(new NullChannel());
                })
                .onRequestComplete(onSuccessfulFinish(request, response, 512))
                .sizeThreshold(1024)
                .setupAsyncParse(request);
    }

    private void thresholdGreater(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    final var currentPart = context.getCurrentPart();
                    assertFalse(currentPart.isFinished());
                    return PartOutput.from(new NullChannel());
                })
                .onRequestComplete(onSuccessfulFinish(request, response, 2048))
                .sizeThreshold(1024)
                .setupAsyncParse(request);
    }

    private void error(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> new EvilOutput("This will cause an error!"))
                .onError((context, throwable) -> response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
                .setupAsyncParse(request);
    }

    private void ioErrorUponError(final HttpServletRequest request) throws ServletException, IOException {
        request.startAsync().setTimeout(500);
        UploadParser.newParser()
                .onRequestComplete(context -> {
                    throw new IOException();
                })
                .onError((context, throwable) -> {
                    throw new ServletException();
                })
                .setupAsyncParse(request);
    }

    private void servletErrorUponError(final HttpServletRequest request) throws ServletException, IOException {
        request.startAsync().setTimeout(500);
        // onError will not be called for ServletException!
        UploadParser.newParser()
                .onRequestComplete(context -> {
                    throw new ServletException();
                })
                .setupAsyncParse(request);
    }

    private void complex(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        if (!UploadParser.isMultipart(request)) {
            throw new ServletException("Not multipart!");
        }

        final var partCounter = new AtomicInteger(0);
        final List<ByteArrayOutputStream> formFields = new ArrayList<>();

        final var expectedContentTypes = Arrays.asList(
                "text/plain",
                "text/plain",
                "text/plain",
                "text/plain",
                "text/plain",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/jpeg");

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    var part = context.getCurrentPart();

                    var detectedType = ClientRequest.tika.detect(new ByteBufferBackedInputStream(buffer), part.getSubmittedFileName());
                    var expectedType = expectedContentTypes.get(partCounter.getAndIncrement());
                    if (expectedType.equals("text/plain")) {
                        assertTrue(detectedType.equals("text/plain") || detectedType.equals("application/octet-stream"));
                    } else {
                        assertEquals(detectedType, expectedType);
                    }

                    var name = part.getName();
                    if (part.isFile()) {
                        if ("".equals(part.getSubmittedFileName())) {
                            throw new IOException("No file was chosen for the form field!");
                        }
                        System.out.println("File field " + name + " with file name "
                                + part.getSubmittedFileName() + " detected!");
                        for (var header : part.getHeaderNames()) {
                            System.out.println(header + " " + part.getHeader(header));
                        }
                        part.getHeaders("content-type");
                        System.out.println(part.getContentType());
                        var baos = new ByteArrayOutputStream();
                        formFields.add(baos);
                        return PartOutput.from(baos);
                    } else {
                        for (var header : part.getHeaderNames()) {
                            System.out.println(header + " " + part.getHeader(header));
                        }
                        System.out.println(part.getContentType());
                        var baos = new ByteArrayOutputStream();
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

                    assertArrayEquals(formFields.get(0).toByteArray(), RequestSupplier.largeFile);
                    assertArrayEquals(formFields.get(1).toByteArray(), RequestSupplier.emptyFile);
                    assertArrayEquals(formFields.get(2).toByteArray(), RequestSupplier.smallFile);
                    assertArrayEquals(formFields.get(3).toByteArray(), RequestSupplier.textValue1.getBytes(ISO_8859_1));
                    assertArrayEquals(formFields.get(4).toByteArray(), RequestSupplier.textValue2.getBytes(ISO_8859_1));

                    context.getUserObject(HttpServletResponse.class).setStatus(HttpServletResponse.SC_OK);
                    context.getRequest().getAsyncContext().complete();
                })
                .onError((context, throwable) -> {
                    System.out.println("Error!");
                    throwable.printStackTrace();
                    response.sendError(500);
                })
                .userObject(response)
                .sizeThreshold(4096)
                .maxPartSize(Long.MAX_VALUE)
                .maxRequestSize(Long.MAX_VALUE)
                .setupAsyncParse(request);
    }

    private static OnRequestComplete onSuccessfulFinish(final HttpServletRequest request, final HttpServletResponse response, final int size) {
        return context -> {
            final var currentPart = context.getCurrentPart();
            assertTrue(currentPart.isFinished());
            assertEquals(size, currentPart.getKnownSize());
            assertTrue(request.isAsyncStarted());
            request.getAsyncContext().complete();
            response.setStatus(200);
        };
    }
}
