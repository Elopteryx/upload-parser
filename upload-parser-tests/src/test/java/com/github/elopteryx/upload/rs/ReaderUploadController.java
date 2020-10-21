package com.github.elopteryx.upload.rs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("upload")
public class ReaderUploadController {

    /**
     * Endpoint to test valid uploads.
     */
    @POST
    @Path("uploadWithReader")
    public Response multipart(final MultiPart multiPart, final List<Part> parts, @UploadParam("filefield1") final Part firstFile) throws IOException {
        assertNotNull(multiPart);
        assertNotNull(parts);
        assertNotNull(firstFile);
        assertEquals(8, multiPart.getParts().size());
        assertTrue(multiPart.getSize() > 0);
        assertFalse(multiPart.getHeaders().isEmpty());
        for (final var part : parts) {
            assertTrue(part.getSize() >= 0);
            if (part.isFile()) {
                if ("".equals(part.getSubmittedFileName())) {
                    throw new IOException("No file was chosen for the form field!");
                }
                part.getHeaders("content-type");
            }
        }
        return Response.status(200).build();
    }

    /**
     * Endpoint to test invalid injection targets.
     */
    @POST
    @Path("uploadWithInvalidParameters")
    public Response invalidParamInjection(@UploadParam("nonExistent") final Part part1, final Part part2, final ByteBuffer buffer, final List<String> names) {
        // These parameters are not valid injection targets, the reader does not support them
        assertNull(part1);
        assertNull(part2);
        assertNull(buffer);
        assertNull(names);
        return Response.status(200).build();
    }

    /**
     * Endpoint to test part size limit checking.
     */
    @POST
    @Path("uploadWithReaderAndPartLimit")
    public Response multipartSizeLimited(@UploadConfig(maxPartSize = 4096) final MultiPart multiPart) {
        // This should be called only when each part size is smaller than the limit
        assertNotNull(multiPart);
        return Response.status(200).build();
    }

    /**
     * Endpoint to test request size limit checking.
     */
    @POST
    @Path("uploadWithReaderAndRequestLimit")
    public Response multipartRequestSizeLimited(@UploadConfig(maxRequestSize = 4096) final MultiPart multiPart) {
        // This should be called only when the request size is smaller than the limit
        assertNotNull(multiPart);
        return Response.status(200).build();
    }

}
