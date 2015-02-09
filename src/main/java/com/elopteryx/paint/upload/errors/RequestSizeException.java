package com.elopteryx.paint.upload.errors;

public class RequestSizeException extends UploadSizeException {

    /**
     * Exception thrown when there is a maximum size limit set for the whole
     * request and it is exceeded for the first time.
     * @param message The message of the exception
     * @param actual The known size at the time of the exception in bytes
     * @param permitted The maximum permitted size in bytes
     */
    public RequestSizeException(String message, long actual, long permitted) {
        super(message, actual, permitted);
    }
}
