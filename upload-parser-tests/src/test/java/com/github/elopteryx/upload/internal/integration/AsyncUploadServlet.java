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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(value = "/async", asyncSupported = true)
public class AsyncUploadServlet extends HttpServlet {

    @Override
    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final var query = request.getQueryString();
        switch (query) {
            case ClientRequest.SIMPLE -> simple(request, response);
            case ClientRequest.THRESHOLD_LESSER -> thresholdLesser(request, response);
            case ClientRequest.THRESHOLD_GREATER -> thresholdGreater(request, response);
            case ClientRequest.ERROR -> error(request, response);
            case ClientRequest.IO_ERROR_UPON_ERROR -> ioErrorUponError(request);
            case ClientRequest.SERVLET_ERROR_UPON_ERROR -> servletErrorUponError(request);
            case ClientRequest.COMPLEX -> complex(request, response);
        }
    }

    private void simple(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    if (context.getPartStreams().size() == 1) {
                        final var dir = ClientRequest.FILE_SYSTEM.getPath("");
                        final var temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(Files.newByteChannel(temp, EnumSet.of(CREATE, TRUNCATE_EXISTING, WRITE)));
                    } else if (context.getPartStreams().size() == 2) {
                        final var dir = ClientRequest.FILE_SYSTEM.getPath("");
                        final var temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(Files.newOutputStream(temp));
                    } else if (context.getPartStreams().size() == 3) {
                        final var dir = ClientRequest.FILE_SYSTEM.getPath("");
                        final var temp = dir.resolve(context.getCurrentPart().getSubmittedFileName());
                        return PartOutput.from(temp);
                    } else {
                        return PartOutput.from(new NullChannel());
                    }
                })
                .onPartEnd(context -> {
                    if (context.getCurrentOutput() != null && context.getCurrentOutput().safeToCast(Channel.class)) {
                        final var channel = context.getCurrentOutput().unwrap(Channel.class);
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

    private static class EvilOutput extends PartOutput {
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
                    final var part = context.getCurrentPart();

                    final var detectedType = ClientRequest.TIKA.detect(new ByteBufferBackedInputStream(buffer), part.getSubmittedFileName());
                    final var expectedType = expectedContentTypes.get(partCounter.getAndIncrement());
                    if ("text/plain".equals(expectedType)) {
                        assertTrue("text/plain".equals(detectedType) || "application/octet-stream".equals(detectedType));
                    } else {
                        assertEquals(detectedType, expectedType);
                    }
                    assertEquals(part.getHeaderNames(), Set.of("content-disposition", "content-type"));
                    if (part.isFile()) {
                        if ("".equals(part.getSubmittedFileName())) {
                            throw new IOException("No file was chosen for the form field!");
                        }
                        assertFalse(part.getHeaders("content-type").isEmpty());
                        assertEquals(part.getContentType(), part.getHeader("content-type"));
                        final var baos = new ByteArrayOutputStream();
                        formFields.add(baos);
                        return PartOutput.from(baos);
                    } else {
                        final var baos = new ByteArrayOutputStream();
                        formFields.add(baos);
                        return PartOutput.from(baos);
                    }
                })
                .onPartEnd(context -> {})
                .onRequestComplete(context -> {
                    assertArrayEquals(formFields.get(0).toByteArray(), RequestSupplier.LARGE_FILE);
                    assertArrayEquals(formFields.get(1).toByteArray(), RequestSupplier.EMPTY_FILE);
                    assertArrayEquals(formFields.get(2).toByteArray(), RequestSupplier.SMALL_FILE);
                    assertArrayEquals(formFields.get(3).toByteArray(), RequestSupplier.TEXT_VALUE_1.getBytes(ISO_8859_1));
                    assertArrayEquals(formFields.get(4).toByteArray(), RequestSupplier.TEXT_VALUE_2.getBytes(ISO_8859_1));

                    context.getUserObject(HttpServletResponse.class).setStatus(HttpServletResponse.SC_OK);
                    context.getRequest().getAsyncContext().complete();
                })
                .onError((context, throwable) -> response.sendError(500))
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
