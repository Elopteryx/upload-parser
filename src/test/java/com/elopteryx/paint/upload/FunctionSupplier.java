package com.elopteryx.paint.upload;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class FunctionSupplier {

    public static OnPartBegin partValidator() {
        return new OnPartBegin() {
            @Override
            public WritableByteChannel apply(UploadContext context, ByteBuffer buffer) throws IOException {
                return null;
            }
        };
    }

    public static OnPartEnd partExecutor() {
        return new OnPartEnd() {
            @Override
            public void accept(UploadContext context) throws IOException {

            }
        };
    }
    
    public static OnRequestComplete requestExecutor() {
        return new OnRequestComplete() {
            @Override
            public void accept(UploadContext context) throws IOException, ServletException {
                
            }
        };
    }
    
    public static OnError errorExecutor() {
        return new OnError() {
            @Override
            public void accept(UploadContext context, Throwable throwable) {

            }
        };
    }
}
