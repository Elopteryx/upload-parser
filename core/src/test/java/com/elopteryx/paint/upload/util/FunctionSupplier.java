package com.elopteryx.paint.upload.util;

import com.elopteryx.paint.upload.OnError;
import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.OnRequestComplete;

public class FunctionSupplier {

    public static OnPartBegin partBeginCallback() {
        return (context, buffer) -> null;
    }

    public static OnPartEnd partEndCallback() {
        return context -> {

        };
    }
    
    public static OnRequestComplete requestCallback() {
        return context -> {

        };
    }
    
    public static OnError errorCallback() {
        return (context, throwable) -> {

        };
    }
}
