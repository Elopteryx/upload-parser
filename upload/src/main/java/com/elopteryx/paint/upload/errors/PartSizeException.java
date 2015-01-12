package com.elopteryx.paint.upload.errors;

public class PartSizeException extends UploadSizeException {

    /**
     * Creates a new instance.
     *
     * @param message   The detail message.
     * @param actual    The actual number of bytes in the request.
     * @param permitted The requests size limit, in bytes.
     */
    public PartSizeException(String message, long actual, long permitted) {
        super(message, actual, permitted);
    }
}
