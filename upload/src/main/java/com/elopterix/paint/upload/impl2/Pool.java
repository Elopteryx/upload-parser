package com.elopterix.paint.upload.impl2;

public class Pool<T> {
    
    Pooled<T> allocate() {
        return new Pooled<>();
    }
}