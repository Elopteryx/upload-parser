package com.github.elopteryx.upload.rs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.elopteryx.upload.PartOutput;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

@Path("upload")
public class ReaderUploadController {

    /**
     * Endpoint to test valid uploads.
     */
    @POST
    @Path("uploadWithReader")
    public Response multipart(MultiPart multiPart, List<Part> parts, @UploadParam("filefield1") Part firstFile) throws IOException {
        assertNotNull(multiPart);
        assertNotNull(parts);
        assertNotNull(firstFile);
        assertEquals(8, multiPart.getParts().size());
        assertTrue(multiPart.getSize() > 0);
        assertFalse(multiPart.getHeaders().isEmpty());
        for (Part part : parts) {
            assertTrue(part.getSize() >= 0);
            if (part.isFile()) {
                String name = part.getName();
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
                PartOutput output = part.getOutPut();
                if (output.safeToCast(ByteArrayOutputStream.class)) {
                    ByteArrayOutputStream bos = output.unwrap(ByteArrayOutputStream.class);
                    System.out.println(bos.toString());
                }
            } else {
                for (String header : part.getHeaderNames()) {
                    System.out.println(header + " " + part.getHeader(header));
                }
                System.out.println(part.getContentType());
                PartOutput output = part.getOutPut();
                if (output.safeToCast(ByteArrayOutputStream.class)) {
                    ByteArrayOutputStream bos = output.unwrap(ByteArrayOutputStream.class);
                    System.out.println(bos.toString());
                }
            }
        }
        return Response.status(200).build();
    }

    /**
     * Endpoint to test invalid injection targets.
     */
    @POST
    @Path("uploadWithInvalidParameters")
    public Response invalidParamInjection(@UploadParam("nonExistent") Part part1, Part part2, ByteBuffer buffer, List<String> names) {
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
    public Response multipartSizeLimited(@UploadConfig(maxPartSize = 4096) MultiPart multiPart) {
        // This should be called only when each part size is smaller than the limit
        assertNotNull(multiPart);
        return Response.status(200).build();
    }

    /**
     * Endpoint to test request size limit checking.
     */
    @POST
    @Path("uploadWithReaderAndRequestLimit")
    public Response multipartRequestSizeLimited(@UploadConfig(maxRequestSize = 4096) MultiPart multiPart) {
        // This should be called only when the request size is smaller than the limit
        assertNotNull(multiPart);
        return Response.status(200).build();
    }

}
