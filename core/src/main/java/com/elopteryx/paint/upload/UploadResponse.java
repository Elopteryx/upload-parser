package com.elopteryx.paint.upload;

import javax.servlet.http.HttpServletResponse;

/**
 * A value holder class, allowing the caller to provide
 * different response objects depending upon the web
 * framework used.
 */
public class UploadResponse {

    /**
     * The value object
     */
    private Object value;

    private UploadResponse() {}

    /**
     * Creates a new instance from the given response object.
     * @param response THe servlet response
     * @return A new UploadResponse instance
     */
    public static UploadResponse from(HttpServletResponse response) {
        UploadResponse wrapper = new UploadResponse();
        wrapper.value = response;
        return wrapper;
    }

    /**
     * Returns whether it is safe to retrieve the value object
     * with the class parameter.
     * @param clazz The class type to check
     * @param <T> Type parameter
     * @return Whether it is safe to cast or not
     */
    public <T> boolean safeToCast(Class<T> clazz) {
        return clazz.isAssignableFrom(value.getClass());
    }

    /**
     * Retrieves the value object, casting it to the
     * given type.
     * @param clazz The class to to cast
     * @param <T> Type parameter
     * @return The stored value object
     */
    public <T> T get(Class<T> clazz) {
        return clazz.cast(value);
    }
}
