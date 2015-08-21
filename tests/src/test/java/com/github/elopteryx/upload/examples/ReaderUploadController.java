package com.github.elopteryx.upload.examples;

import com.github.elopteryx.upload.rs.MultiPart;
import com.github.elopteryx.upload.rs.Part;
import com.github.elopteryx.upload.rs.UploadConfig;
import com.github.elopteryx.upload.rs.UploadParam;

import javax.servlet.ServletException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;

/**
 * Example controller class for the Jax-Rs library. Uses the reader to implicitly parse the upload.
 */
@Path("upload")
public class ReaderUploadController {

    /**
     * Example endpoint for the reader, the reader injects the object representing the whole
     * multipart request.
     * @param multiPart The multipart request
     * @throws IOException If an error occurred with the I/O
     * @throws ServletException If an error occurred with the servlet
     */
    @POST
    @Path("upload1")
    public void multiPart(@UploadConfig(sizeThreshold = 4096)MultiPart multiPart) throws IOException, ServletException {
        multiPart.getParts();
        multiPart.getHeaders();
        multiPart.getSize();
        // ...
    }

    /**
     * Example endpoint for the reader, the reader injects the part objects as method parameters.
     * @param part1 The first part, with the given form name
     * @param part2 The second part, with the given form name
     * @param part3 The third part, with the given form name
     * @throws IOException If an error occurred with the I/O
     * @throws ServletException If an error occurred with the servlet
     */
    @POST
    @Path("upload2")
    public void separateParts(@UploadParam("text1")Part part1,
                              @UploadParam("text2")Part part2,
                              @UploadParam("file")Part part3
    ) throws IOException, ServletException {
        // ...
    }
}
