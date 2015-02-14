package com.elopteryx.paint.upload;

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
