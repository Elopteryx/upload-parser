package com.elopteryx.paint.upload;

public interface OnError {

    void accept(UploadContext context, Throwable throwable);
    
}
