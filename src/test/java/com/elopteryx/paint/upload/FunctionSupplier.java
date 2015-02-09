package com.elopteryx.paint.upload;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class FunctionSupplier {

    public static OnPartBegin partBeginCallback() {
        return new OnPartBegin() {
            @Override
            public WritableByteChannel onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
                return null;
            }
        };
    }

    public static OnPartEnd partEndCallback() {
        return new OnPartEnd() {
            @Override
            public void onPartEnd(UploadContext context) throws IOException {

            }
        };
    }
    
    public static OnRequestComplete requestCallback() {
        return new OnRequestComplete() {
            @Override
            public void onRequestComplete(UploadContext context) throws IOException, ServletException {
                
            }
        };
    }
    
    public static OnError errorCallback() {
        return new OnError() {
            @Override
            public void onError(UploadContext context, Throwable throwable) {

            }
        };
    }
}
