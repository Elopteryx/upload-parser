package com.github.elopteryx.upload.rs;

import com.github.elopteryx.upload.OnError;
import com.github.elopteryx.upload.OnPartBegin;
import com.github.elopteryx.upload.OnRequestComplete;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.util.NullChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("upload")
public class AsyncUploadController implements OnPartBegin, OnRequestComplete, OnError {

    /**
     * This endpoint serves multipart requests, using a newly created upload parser.
     * @param request The servlet request
     * @param asyncResponse The Jax-Rs async response
     * @throws IOException If an error occurred with the I/O
     * @throws ServletException If an error occurred with the servlet
     */
    @POST
    @Path("uploadWithParser")
    public void multipart(@Context HttpServletRequest request, @Suspended final AsyncResponse asyncResponse) throws IOException, ServletException {
        UploadParser.newParser()
                .onPartBegin(this)
                .onRequestComplete(this)
                .onError(this)
                .userObject(asyncResponse)
                .setupAsyncParse(request);
    }

    @Nonnull
    @Override
    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
        return PartOutput.from(new NullChannel());
    }


    @Override
    public void onRequestComplete(UploadContext context) throws IOException, ServletException {
        context.getUserObject(AsyncResponse.class).resume(Response.status(200).build());
    }

    @Override
    public void onError(UploadContext context, Throwable throwable) {
        context.getUserObject(AsyncResponse.class).resume(Response.status(500).build());
    }
}
