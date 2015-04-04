package com.elopteryx.paint.upload.rs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.elopteryx.paint.upload.PartOutput;

import javax.servlet.ServletException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Path("upload")
public class ReaderUploadController {

    @POST
    @Path("uploadWithReader")
    public Response multipart(MultiPart multiPart) throws IOException, ServletException {
        assertTrue(multiPart.getParts().size() == 5);
        assertTrue(multiPart.getSize() > 0);
        assertFalse(multiPart.getHeaders().isEmpty());
        for (Part part : multiPart.getParts()) {
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

    @POST
    @Path("uploadWithReaderAndPartLimit")
    public Response multipartSizeLimited(@UploadConfig(maxPartSize = 4096) MultiPart multiPart) throws IOException, ServletException {
        // This should be called only when each part size is smaller than the limit
        return Response.status(200).build();
    }

    @POST
    @Path("uploadWithReaderAndRequestLimit")
    public Response multipartRequestSizeLimited(@UploadConfig(maxRequestSize = 4096) MultiPart multiPart) throws IOException, ServletException {
        // This should be called only when the request size is smaller than the limit
        return Response.status(200).build();
    }

}
