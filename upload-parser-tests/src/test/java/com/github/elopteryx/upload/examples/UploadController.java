package com.github.elopteryx.upload.examples;

import com.github.elopteryx.upload.OnError;
import com.github.elopteryx.upload.OnPartBegin;
import com.github.elopteryx.upload.OnPartEnd;
import com.github.elopteryx.upload.OnRequestComplete;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.util.NullChannel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Example controller class for the Jax-Rs library. Uses the parser directly.
 */
@Path("upload")
public class UploadController implements OnPartBegin, OnPartEnd, OnRequestComplete, OnError {

    /**
     * Example for using the parser in a Jax-Rs endpoint.
     * @param request The servlet request
     * @param asyncResponse Jax-Rs async response object
     * @throws IOException If an error occurred with the I/O
     * @throws ServletException If an error occurred with the servlet
     */
    @POST
    @Path("upload")
    public void multipart(@Context HttpServletRequest request, @Suspended final AsyncResponse asyncResponse) throws IOException, ServletException {
        UploadParser.newParser()
                .onPartBegin(this)
                .onPartEnd(this)
                .onRequestComplete(this)
                .onError(this)
                .userObject(asyncResponse)
                .setupAsyncParse(request);
    }

    @Override
    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) {
        // Your business logic here, check the part, you can use the bytes in the buffer to check
        // the real mime type, then return with a channel, stream or path to write the part
        return PartOutput.from(new NullChannel());
    }

    @Override
    public void onPartEnd(UploadContext context) {
        // Your business logic here
    }

    @Override
    public void onRequestComplete(UploadContext context) {
        // Your business logic here, send a response to the client
        context.getUserObject(AsyncResponse.class).resume(Response.ok().build());
    }

    @Override
    public void onError(UploadContext context, Throwable throwable) {
        // Your business logic here, handle the error
        context.getUserObject(AsyncResponse.class).resume(Response.serverError().build());
    }
}
