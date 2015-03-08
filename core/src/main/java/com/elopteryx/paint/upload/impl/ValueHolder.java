package com.elopteryx.paint.upload.impl;

/**
 * Base value holder class. It allows the callers to return
 * with different types of objects, which are valid in their
 * own environment, and can be used for the same purpose, but
 * are otherwise unrelated when it comes to inheritance.
 *
 * <p>An example of this are the channels and streams, instances of
 * them can be used to write out the parts of the request, but
 * they don't inherit from each other.
 *
 * <p>This class is in the internal package because the callers
 * should not import this directly.
 */
public class ValueHolder {

    /**
     * The value object
     */
    protected Object value;

    protected ValueHolder() {
        // No need to allow public access
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
     * @param clazz The class to cast
     * @param <T> Type parameter
     * @return The stored value object
     */
    public <T> T get(Class<T> clazz) {
        return clazz.cast(value);
    }
}
