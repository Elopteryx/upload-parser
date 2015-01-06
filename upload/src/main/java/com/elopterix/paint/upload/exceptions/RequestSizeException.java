package com.elopterix.paint.upload.exceptions;

public class RequestSizeException extends UploadSizeException {

    /**
     * Creates a new instance.
     *
     * @param message   The detail message.
     * @param actual    The actual number of bytes in the request.
     * @param permitted The requests size limit, in bytes.
     */
    public RequestSizeException(String message, long actual, long permitted) {
        super(message, actual, permitted);
    }
}
