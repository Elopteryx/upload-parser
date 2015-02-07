package com.elopteryx.paint.upload;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import static java.util.Objects.requireNonNull;

public class MockServletInputStream extends ServletInputStream {

    private final InputStream sourceStream;

    private boolean ready;

    private boolean finished;
    
    private ReadListener readListener;


    public MockServletInputStream(InputStream sourceStream) {
        this.sourceStream = requireNonNull(sourceStream, "Source InputStream must not be null");
    }

    public InputStream getSourceStream() {
        return this.sourceStream;
    }


    @Override
    public int read() throws IOException {
        return this.sourceStream.read();
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.sourceStream.close();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        this.readListener = readListener;
    }
}