package com.elopteryx.paint.upload.errors;

public class PartSizeException extends UploadSizeException {

    /**
     * Exception thrown when there is a maximum size limit set for the individual
     * parts and it is exceeded for the first time.
     * @param message The message of the exception
     * @param actual The known size at the time of the exception
     * @param permitted The maximum permitted size
     */
    public PartSizeException(String message, long actual, long permitted) {
        super(message, actual, permitted);
    }
}
