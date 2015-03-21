package com.elopteryx.paint.upload;

import static org.mockito.Mockito.when;
import static com.elopteryx.paint.upload.util.Servlets.newRequest;
import static com.elopteryx.paint.upload.util.Servlets.newResponse;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.elopteryx.paint.upload.impl.AsyncUploadParser;
import com.elopteryx.paint.upload.impl.BlockingUploadParser;
import com.elopteryx.paint.upload.impl.NullChannel;
import com.elopteryx.paint.upload.util.MockAsyncContext;

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
import javax.servlet.ServletException;
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
        UploadParser.newAsyncParser(request).userObject(UploadResponse.from(newResponse()));
    }

    @Test(expected = ServletException.class)
    public void invalid_content_type_blocking() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.getContentType()).thenReturn("text/plain;charset=UTF-8");
        assertFalse(UploadParser.isMultipart(request));
        UploadParser.newBlockingParser(request).userObject(UploadResponse.from(newResponse()));
    }

    @Test
    public void create_async_parser() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.isAsyncSupported()).thenReturn(true);

        UploadParser asyncParser = UploadParser.newAsyncParser(request).userObject(UploadResponse.from(newResponse()));
        assertThat(asyncParser, instanceOf(AsyncUploadParser.class));
    }

    @Test
    public void create_blocking_parser() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();
        
        when(request.isAsyncSupported()).thenReturn(false);

        UploadParser blockingParser = UploadParser.newBlockingParser(request).userObject(UploadResponse.from(response));
        assertThat(blockingParser, instanceOf(BlockingUploadParser.class));
    }

    @Test
    public void use_the_full_api_async() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.startAsync()).thenReturn(new MockAsyncContext(request, response));

        UploadParser.newAsyncParser(request)
                .onPartBegin(this)
                .onPartEnd(this)
                .onRequestComplete(this)
                .onError(this)
                .sizeThreshold(1024 * 1024 * 10)
                .maxPartSize(1024 * 1024 * 50)
                .maxRequestSize(1024 * 1024 * 50)
                .setupAsyncParse();
    }

    @Test
    public void use_the_full_api_blocking() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.startAsync()).thenReturn(new MockAsyncContext(request, response));

        UploadParser.newBlockingParser(request)
                .onPartBegin(this)
                .onPartEnd(this)
                .onRequestComplete(this)
                .onError(this);
    }

    @Test
    public void output_channel() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.startAsync()).thenReturn(new MockAsyncContext(request, response));

        UploadParser.newBlockingParser(request)
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
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.startAsync()).thenReturn(new MockAsyncContext(request, response));

        UploadParser.newBlockingParser(request)
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
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.startAsync()).thenReturn(new MockAsyncContext(request, response));

        UploadParser.newBlockingParser(request)
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
