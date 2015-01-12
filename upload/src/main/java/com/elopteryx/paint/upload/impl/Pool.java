package com.elopteryx.paint.upload.impl;

public class Pool<T> {
    
    Pooled<T> allocate() {
        return new Pooled<>();
    }
}