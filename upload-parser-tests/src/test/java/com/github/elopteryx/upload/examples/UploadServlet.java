package com.github.elopteryx.upload.examples;

import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Example servlet class for the core library.
 */
@WebServlet(value = "/UploadServlet", asyncSupported = true)
public class UploadServlet extends HttpServlet {

    /**
     * Directory where uploaded files will be saved, its relative to
     * the web application directory.
     */
    private static final String UPLOAD_DIR = "uploads";

    protected void doPost(HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        var applicationPath = request.getServletContext().getRealPath("");
        final var uploadFilePath = Paths.get(applicationPath, UPLOAD_DIR);

        if (!Files.isDirectory(uploadFilePath)) {
            Files.createDirectories(uploadFilePath);
        }

        // Check that we have a file upload request
        if (!UploadParser.isMultipart(request)) {
            return;
        }

        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    var part = context.getCurrentPart();
                    var path = uploadFilePath.resolve(part.getSubmittedFileName());
                    return PartOutput.from(path);
                })
                .onRequestComplete(context -> context.getUserObject(HttpServletResponse.class).setStatus(200))
                .onError((context, throwable) -> {
                    throwable.printStackTrace();
                    response.sendError(500);
                })
                .sizeThreshold(4096)
                .maxBytesUsed(8092)
                .maxPartSize(1024 * 1024 * 25)
                .maxRequestSize(1024 * 1024 * 500)
                .setupAsyncParse(request);
    }
}
