package com.elopteryx.paint.upload;

@FunctionalInterface
public interface OnError {

    void accept(UploadContext context, Throwable throwable);
    
}
