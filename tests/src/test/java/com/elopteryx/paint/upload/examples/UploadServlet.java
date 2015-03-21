package com.elopteryx.paint.upload.examples;

import com.elopteryx.paint.upload.UploadParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

        String applicationPath = request.getServletContext().getRealPath("");
        final Path uploadFilePath = Paths.get(applicationPath, UPLOAD_DIR);

        if (!Files.isDirectory(uploadFilePath)) {
            Files.createDirectories(uploadFilePath);
        }

        // Check that we have a file upload request
        if (!UploadParser.isMultipart(request)) {
            return;
        }

        UploadParser.newParser()
//                .onPartBegin((context, buffer) -> {
//                    PartStream part = context.getCurrentPart();
//                    Path path = uploadFilePath.resolve(part.getSubmittedFileName());
//                    return PartOutput.from(path);
//                })
//                .onRequestComplete(context -> context.getUserObject(HttpServletResponse.class).setStatus(200))
//                .onError((context, throwable) -> {
//                    throwable.printStackTrace();
//                    context.getUserObject(HttpServletResponse.class).sendError(500);
//                })
                .sizeThreshold(4096)
                .maxPartSize(1024 * 1024 * 25)
                .maxRequestSize(1024 * 1024 * 500)
                .setupAsyncParse(request);
    }
}
