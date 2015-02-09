package com.elopteryx.paint.upload.errors;

/**
 * Base class for the size related exceptions.
 */
public abstract class UploadSizeException extends RuntimeException {

    /**
     * The known size.
     */
    private final long actual;

    /**
     * The maximum permitted size.
     */
    private final long permitted;

    /**
     * Exception thrown when there is a maximum size limit and it is exceeded for the first time
     * @param message The message of the exception
     * @param actual The known size at the time of the exception in bytes
     * @param permitted The maximum permitted size in bytes
     */
    UploadSizeException(String message, long actual, long permitted) {
        super(message);
        this.actual = actual;
        this.permitted = permitted;
    }

    /**
     * Returns the actual size.
     *
     * @return The actual size.
     */
    public long getActualSize() {
        return actual;
    }

    /**
     * Returns the permitted size.
     *
     * @return The permitted size.
     */
    public long getPermittedSize() {
        return permitted;
    }
}
