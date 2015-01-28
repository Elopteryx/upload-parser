package com.elopteryx.paint.upload;

/**
 * A functional interface. An implementation of it must be passed in the
 * UploadParser#onError method to call it after an error occurs.
 */
public interface OnError {

    /**
     * The consumer function to implement.
     * @param context The upload context
     * @param throwable The error that occurred
     */
    void accept(UploadContext context, Throwable throwable);
    
}
