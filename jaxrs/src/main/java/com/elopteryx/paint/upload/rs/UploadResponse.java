package com.elopteryx.paint.upload.rs;

import javax.ws.rs.container.AsyncResponse;

public class UploadResponse extends com.elopteryx.paint.upload.UploadResponse {

    protected UploadResponse() {
        // No need to allow public access
    }

    /**
     * Creates a new instance from the given response object.
     * @param response The servlet response
     * @return A new UploadResponse instance
     */
    public static UploadResponse from(AsyncResponse response) {
        UploadResponse wrapper = new UploadResponse();
        wrapper.value = response;
        return wrapper;
    }
}
