package com.elopteryx.paint.upload.rs;

import com.elopteryx.paint.upload.UploadContext;

import javax.servlet.ServletException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("upload")
public class ReaderUploadController {

    @POST
    @Path("uploadWithReader")
    public Response multipart(UploadContext context) throws IOException, ServletException {
        return Response.status(200).build();
    }

    @POST
    @Path("uploadWithReaderAndPartLimit")
    public Response multipartSizeLimited(@UploadConfig(maxPartSize = 4096) UploadContext context) throws IOException, ServletException {
        // This should be called only when each part size is smaller than the limit
        return Response.status(200).build();
    }

    @POST
    @Path("uploadWithReaderAndRequestLimit")
    public Response multipartRequestSizeLimited(@UploadConfig(maxRequestSize = 4096) UploadContext context) throws IOException, ServletException {
        // This should be called only when the request size is smaller than the limit
        return Response.status(200).build();
    }

}
