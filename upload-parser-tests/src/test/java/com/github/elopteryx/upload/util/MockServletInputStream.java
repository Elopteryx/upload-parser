package com.github.elopteryx.upload.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

public class MockServletInputStream extends ServletInputStream {

    private static final String FILE_NAME = "foo.txt";
    private static final String REQUEST_DATA =
            "-----1234\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + FILE_NAME + "\"\r\n"
                    + "Content-Type: text/whatever\r\n"
                    + "\r\n"
                    + "This is the content of the file\n"
                    + "\r\n"
                    + "-----1234\r\n"
                    + "Content-Disposition: form-data; name=\"field\"\r\n"
                    + "\r\n"
                    + "fieldValue\r\n"
                    + "-----1234\r\n"
                    + "Content-Disposition: form-data; name=\"multi\"\r\n"
                    + "\r\n"
                    + "value1\r\n"
                    + "-----1234\r\n"
                    + "Content-Disposition: form-data; name=\"multi\"\r\n"
                    + "\r\n"
                    + "value2\r\n"
                    + "-----1234--\r\n";
    
    private final ByteArrayInputStream sourceStream;
    
    private ReadListener readListener;

    MockServletInputStream() {
        this.sourceStream = new ByteArrayInputStream(REQUEST_DATA.getBytes(StandardCharsets.US_ASCII));
    }

    public void onDataAvailable() throws IOException {
        readListener.onDataAvailable();
    }

    @Override
    public int read() {
        return this.sourceStream.read();
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.sourceStream.close();
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(final ReadListener readListener) {
        this.readListener = readListener;
    }
}