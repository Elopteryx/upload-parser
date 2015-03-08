package com.elopteryx.paint.upload;

import com.elopteryx.paint.upload.impl.ValueHolder;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * A value holder class, allowing the caller to provide
 * different response objects depending upon the web
 * framework used.
 */
public class UploadResponse extends ValueHolder {

    protected UploadResponse() {
        // No need to allow public access
    }

    /**
     * Creates a new instance from the given response object.
     * @param response The servlet response
     * @return A new UploadResponse instance
     */
    public static UploadResponse from(@Nonnull HttpServletResponse response) {
        UploadResponse wrapper = new UploadResponse();
        wrapper.value = response;
        return wrapper;
    }
}
