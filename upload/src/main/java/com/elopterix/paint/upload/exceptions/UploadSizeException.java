package com.elopterix.paint.upload.exceptions;

abstract class UploadSizeException extends RuntimeException {

    /**
     * The actual size.
     */
    private final long actual;

    /**
     * The maximum permitted size.
     */
    private final long permitted;

    /**
     * Creates a new instance.
     *
     * @param message   The detail message.
     * @param actual    The actual number of bytes.
     * @param permitted The size limit, in bytes.
     */
    UploadSizeException(String message, long actual, long permitted) {
        super(message);
        this.actual = actual;
        this.permitted = permitted;
    }

    /**
     * Retrieves the actual size.
     *
     * @return The actual size.
     */
    public long getActualSize() {
        return actual;
    }

    /**
     * Retrieves the permitted size.
     *
     * @return The permitted size.
     */
    public long getPermittedSize() {
        return permitted;
    }
}
