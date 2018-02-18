package com.github.elopteryx.upload.internal.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.elopteryx.upload.OnRequestComplete;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.PartStream;
import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.util.NullChannel;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.channels.Channel;

@WebServlet(value = "/blocking")
public class BlockingUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String query = request.getQueryString();
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
            default:
                break;
        }

    }

    private void simple(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        UploadContext context = UploadParser.newParser()
                .onPartEnd(context1 -> {
                    if (context1.getCurrentOutput() != null && context1.getCurrentOutput().safeToCast(Channel.class)) {
                        Channel channel = context1.getCurrentOutput().unwrap(Channel.class);
                        if (channel.isOpen()) {
                            // The parser should close it
                            fail();
                        }
                    }
                })
                .onRequestComplete(context1 -> response.setStatus(200))
                .doBlockingParse(request);
        assertTrue(context.getPartStreams().size() == 8);
    }

    private void thresholdLesser(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    final PartStream currentPart = context.getCurrentPart();
                    assertTrue(currentPart.isFinished());
                    return PartOutput.from(new NullChannel());
                })
                .onRequestComplete(onSuccessfulFinish(request, response, 512))
                .sizeThreshold(1024)
                .doBlockingParse(request);
    }

    private void thresholdGreater(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    final PartStream currentPart = context.getCurrentPart();
                    assertFalse(currentPart.isFinished());
                    return PartOutput.from(new NullChannel());
                })
                .onRequestComplete(onSuccessfulFinish(request, response, 2048))
                .sizeThreshold(1024)
                .doBlockingParse(request);
    }

    private void error(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onError((context, throwable) -> response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
                .maxRequestSize(4096)
                .doBlockingParse(request);
    }

    private static OnRequestComplete onSuccessfulFinish(HttpServletRequest request, HttpServletResponse response, int size) {
        return context -> {
            final PartStream currentPart = context.getCurrentPart();
            assertTrue(currentPart.isFinished());
            assertEquals(size, currentPart.getKnownSize());
            assertFalse(request.isAsyncStarted());
            response.setStatus(200);
        };
    }
}
