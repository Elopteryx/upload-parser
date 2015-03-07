package com.elopteryx.paint.upload.rs;

import javax.annotation.Nonnull;
import javax.ws.rs.container.AsyncResponse;

/**
 * Jax-Rs specific version of the response wrapper. This class can be used
 * to wrap an AsyncResponse to retrieve it later if the instance reference
 * is not available.
 */
public class UploadResponse extends com.elopteryx.paint.upload.UploadResponse {

    private UploadResponse() {
        // No need to allow public access
    }

    /**
     * Creates a new instance from the given response object.
     * @param response The servlet response
     * @return A new UploadResponse instance
     */
    public static UploadResponse from(@Nonnull AsyncResponse response) {
        UploadResponse wrapper = new UploadResponse();
        wrapper.value = response;
        return wrapper;
    }
}
