package com.github.elopteryx.upload.internal.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.elopteryx.upload.OnRequestComplete;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.util.NullChannel;

import java.io.IOException;
import java.nio.channels.Channel;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/blocking")
public class BlockingUploadServlet extends HttpServlet {

    @Override
    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final var query = request.getQueryString();
        switch (query) {
            case ClientRequest.SIMPLE -> simple(request, response);
            case ClientRequest.THRESHOLD_LESSER -> thresholdLesser(request, response);
            case ClientRequest.THRESHOLD_GREATER -> thresholdGreater(request, response);
            case ClientRequest.ERROR -> error(request, response);
        }
    }

    private void simple(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final var context = UploadParser.newParser()
                .onPartEnd(context1 -> {
                    if (context1.getCurrentOutput() != null && context1.getCurrentOutput().safeToCast(Channel.class)) {
                        final var channel = context1.getCurrentOutput().unwrap(Channel.class);
                        if (channel.isOpen()) {
                            fail("The parser should close it!");
                        }
                    }
                })
                .onRequestComplete(context1 -> response.setStatus(200))
                .doBlockingParse(request);
        assertEquals(8, context.getPartStreams().size());
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
                .doBlockingParse(request);
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
                .doBlockingParse(request);
    }

    private void error(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onError((context, throwable) -> response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
                .maxRequestSize(4096)
                .doBlockingParse(request);
    }

    private static OnRequestComplete onSuccessfulFinish(final HttpServletRequest request, final HttpServletResponse response, final int size) {
        return context -> {
            final var currentPart = context.getCurrentPart();
            assertTrue(currentPart.isFinished());
            assertEquals(size, currentPart.getKnownSize());
            assertFalse(request.isAsyncStarted());
            response.setStatus(200);
        };
    }
}
