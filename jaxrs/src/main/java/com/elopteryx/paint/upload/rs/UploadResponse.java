package com.elopteryx.paint.upload.rs;

import javax.ws.rs.container.AsyncResponse;

public class UploadResponse {

    private Object value;

    private UploadResponse() {
        // No need to allow public access
    }

    public static UploadResponse from(AsyncResponse response) {
        UploadResponse wrapper = new UploadResponse();
        wrapper.value = response;
        return wrapper;
    }

    public <T> boolean safeToCast(Class<T> clazz) {
        return clazz.isAssignableFrom(value.getClass());
    }

    public <T> T getValue(Class<T> clazz) {
        return clazz.cast(value);
    }
}
