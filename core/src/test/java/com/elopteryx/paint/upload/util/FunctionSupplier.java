package com.elopteryx.paint.upload.util;

import com.elopteryx.paint.upload.OnError;
import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.OnRequestComplete;
import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.UploadContext;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class FunctionSupplier {

    public static OnPartBegin partBeginCallback() {
        return new OnPartBegin() {
            @Override
            public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
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
