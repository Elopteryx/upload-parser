package io.undertow.util;

import java.nio.ByteBuffer;

public class Pooled<T> implements AutoCloseable {
    
    private ByteBuffer buffer = ByteBuffer.allocate(8192);
    
    public void discard() {
        buffer = null;
    }

    public void free() {
        buffer = null;
    }

    T getResource() throws IllegalStateException {
        return (T)buffer;
        
    }

    public void close() {
        buffer = null;
    }
}
