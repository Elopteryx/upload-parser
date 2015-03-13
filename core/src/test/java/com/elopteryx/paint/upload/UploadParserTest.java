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
import com.elopteryx.paint.upload.util.MockAsyncContext;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UploadParserTest implements OnPartBegin, OnPartEnd, OnRequestComplete, OnError {

    @Test(expected = ServletException.class)
    public void valid_and_invalid_content_type() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.getContentType()).thenReturn("multipart/");
        assertTrue(UploadParser.isMultipart(request));

        when(request.getContentType()).thenReturn("text/plain;charset=UTF-8");
        assertFalse(UploadParser.isMultipart(request));
        UploadParser.newAsyncParser(request).withResponse(UploadResponse.from(newResponse()));
    }

    @Test
    public void create_async_parser() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.isAsyncSupported()).thenReturn(true);

        UploadParser asyncParser = UploadParser.newAsyncParser(request).withResponse(UploadResponse.from(newResponse()));
        assertThat(asyncParser, instanceOf(AsyncUploadParser.class));
    }

    @Test
    public void create_blocking_parser() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();
        
        when(request.isAsyncSupported()).thenReturn(false);

        UploadParser blockingParser = UploadParser.newBlockingParser(request).withResponse(UploadResponse.from(response));
        assertThat(blockingParser, instanceOf(BlockingUploadParser.class));
    }

    @Test
    public void use_the_full_api() throws Exception {
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

        UploadParser.newBlockingParser(request)
                .onPartBegin(this)
                .onPartEnd(this)
                .onRequestComplete(this)
                .onError(this);
    }

    @Override
    @Nonnull
    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
        if (new Random().nextInt() % 2 == 0) {
            return PartOutput.from(Files.newByteChannel(Paths.get("")));
        } else {
            return PartOutput.from(Files.newOutputStream(Paths.get("")));
        }
    }

    @Override
    public void onPartEnd(UploadContext context) throws IOException {}

    @Override
    public void onRequestComplete(UploadContext context) throws IOException, ServletException {}

    @Override
    public void onError(UploadContext context, Throwable throwable) {}
}
