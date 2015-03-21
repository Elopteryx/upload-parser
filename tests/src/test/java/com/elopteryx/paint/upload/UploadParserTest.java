package com.elopteryx.paint.upload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.elopteryx.paint.upload.util.Servlets.newRequest;
import static com.elopteryx.paint.upload.util.Servlets.newResponse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.elopteryx.paint.upload.internal.NullChannel;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UploadParserTest implements OnPartBegin, OnPartEnd, OnRequestComplete, OnError {

    private FileSystem fileSystem;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void valid_content_type() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.getContentType()).thenReturn("multipart/");
        assertTrue(UploadParser.isMultipart(request));
    }

    @Test(expected = ServletException.class)
    public void invalid_content_type_async() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.getContentType()).thenReturn("text/plain;charset=UTF-8");
        assertFalse(UploadParser.isMultipart(request));
        UploadParser.newParser().userObject(newResponse()).setupAsyncParse(request);
    }

    @Test(expected = ServletException.class)
    public void invalid_content_type_blocking() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.getContentType()).thenReturn("text/plain;charset=UTF-8");
        assertFalse(UploadParser.isMultipart(request));
        UploadParser.newParser().userObject(newResponse()).doBlockingParse(request);
    }

    @Test
    public void use_the_full_api() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.startAsync()).thenReturn(mock(AsyncContext.class));
        when(request.getInputStream()).thenReturn(mock(ServletInputStream.class));

        UploadParser.newParser()
                .onPartBegin(this)
                .onPartEnd(this)
                .onRequestComplete(this)
                .onError(this)
                .userObject(response)
                .sizeThreshold(1024 * 1024 * 10)
                .maxPartSize(1024 * 1024 * 50)
                .maxRequestSize(1024 * 1024 * 50)
                .setupAsyncParse(request);
    }

    @Test
    public void output_channel() throws Exception {
        UploadParser.newParser()
                .onPartBegin(new OnPartBegin() {
                    @Nonnull
                    @Override
                    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
                        Path test = fileSystem.getPath("test1");
                        Files.createFile(test);
                        return PartOutput.from(Files.newByteChannel(test));
                    }
                });
    }

    @Test
    public void output_stream() throws Exception {
        UploadParser.newParser()
                .onPartBegin(new OnPartBegin() {
                    @Nonnull
                    @Override
                    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
                        Path test = fileSystem.getPath("test2");
                        Files.createFile(test);
                        return PartOutput.from(Files.newOutputStream(test));
                    }
                });
    }

    @Test
    public void output_path() throws Exception {
        UploadParser.newParser()
                .onPartBegin(new OnPartBegin() {
                    @Nonnull
                    @Override
                    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
                        Path test = fileSystem.getPath("test2");
                        Files.createFile(test);
                        return PartOutput.from(test);
                    }
                });
    }

    @Test
    public void use_with_custom_object() throws Exception {
        UploadParser.newParser()
                .userObject(newResponse())
                .onPartBegin(new OnPartBegin() {
                    @Nonnull
                    @Override
                    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
                        Path test = fileSystem.getPath("test2");
                        Files.createFile(test);
                        return PartOutput.from(test);
                    }
                })
                .onRequestComplete(new OnRequestComplete() {
                    @Override
                    public void onRequestComplete(UploadContext context) throws IOException, ServletException {
                        context.getUserObject(HttpServletResponse.class).setStatus(HttpServletResponse.SC_OK);
                    }
                });
    }

    @Override
    @Nonnull
    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
        return PartOutput.from(new NullChannel());
    }

    @Override
    public void onPartEnd(UploadContext context) throws IOException {}

    @Override
    public void onRequestComplete(UploadContext context) throws IOException, ServletException {}

    @Override
    public void onError(UploadContext context, Throwable throwable) {}
}
