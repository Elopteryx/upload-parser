package com.github.elopteryx.upload.internal.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.UploadParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.channels.Channel;

@WebServlet(value = "/blocking", asyncSupported = false)
public class BlockingUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String query = request.getQueryString();
        switch (query) {
            case Constants.SIMPLE:
                simple(request, response);
                break;
            case Constants.ERROR:
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
        assertTrue(context.getPartStreams().size() == 5);
    }

    private void error(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        UploadParser.newParser()
                .onError((context, throwable) -> response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
                .maxRequestSize(4096)
                .doBlockingParse(request);
    }
}
