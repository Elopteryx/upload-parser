package io.undertow.util;

public class Pool<T> {
    
    Pooled<T> allocate() {
        return new Pooled<>();
    }
}