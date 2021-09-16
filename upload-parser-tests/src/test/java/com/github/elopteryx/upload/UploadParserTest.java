package com.github.elopteryx.upload;

import static com.github.elopteryx.upload.util.Servlets.newRequest;
import static com.github.elopteryx.upload.util.Servlets.newResponse;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.elopteryx.upload.util.NullChannel;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletResponse;

class UploadParserTest implements OnPartBegin, OnPartEnd, OnRequestComplete, OnError {

    private static FileSystem fileSystem;

    @BeforeAll
    static void setUp() {
        fileSystem = Jimfs.newFileSystem();
    }

    @Test
    void valid_content_type() throws Exception {
        final var request = newRequest();

        when(request.getContentType()).thenReturn("multipart/");
        assertTrue(UploadParser.isMultipart(request));
    }

    @Test
    void invalid_numeric_arguments() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> UploadParser.newParser().sizeThreshold(-1)),
                () -> assertThrows(IllegalArgumentException.class, () -> UploadParser.newParser().maxPartSize(-1)),
                () -> assertThrows(IllegalArgumentException.class, () -> UploadParser.newParser().maxRequestSize(-1)),
                () -> assertThrows(IllegalArgumentException.class, () -> UploadParser.newParser().maxBytesUsed(-1))
        );
    }

    @Test
    void invalid_content_type_async() throws Exception {
        final var request = newRequest();

        when(request.getContentType()).thenReturn("text/plain;charset=UTF-8");
        assertFalse(UploadParser.isMultipart(request));
        assertThrows(IllegalArgumentException.class, () -> UploadParser.newParser().userObject(newResponse()).setupAsyncParse(request));
    }

    @Test
    void invalid_content_type_blocking() throws Exception {
        final var request = newRequest();

        when(request.getContentType()).thenReturn("text/plain;charset=UTF-8");
        assertFalse(UploadParser.isMultipart(request));
        assertThrows(IllegalArgumentException.class, () -> UploadParser.newParser().userObject(newResponse()).doBlockingParse(request));
    }

    @Test
    void use_the_full_api() throws Exception {
        final var request = newRequest();
        final var response = newResponse();

        when(request.startAsync()).thenReturn(mock(AsyncContext.class));
        when(request.getInputStream()).thenReturn(mock(ServletInputStream.class));

        UploadParser.newParser()
                .onPartBegin(this)
                .onPartEnd(this)
                .onRequestComplete(this)
                .onError(this)
                .userObject(response)
                .maxBytesUsed(4096)
                .sizeThreshold(1024 * 1024 * 10)
                .maxPartSize(1024 * 1024 * 50)
                .maxRequestSize(1024 * 1024 * 50)
                .setupAsyncParse(request);
    }

    @Test
    void output_channel() {
        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    final var test = fileSystem.getPath("test1");
                    Files.createFile(test);
                    return PartOutput.from(Files.newByteChannel(test));
                });
    }

    @Test
    void output_stream() {
        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    final var test = fileSystem.getPath("test2");
                    Files.createFile(test);
                    return PartOutput.from(Files.newOutputStream(test));
                });
    }

    @Test
    void output_path() {
        UploadParser.newParser()
                .onPartBegin((context, buffer) -> {
                    final var test = fileSystem.getPath("test2");
                    Files.createFile(test);
                    return PartOutput.from(test);
                });
    }

    @Test
    void use_with_custom_object() {
        UploadParser.newParser()
                .userObject(newResponse())
                .onPartBegin((context, buffer) -> {
                    final var test = fileSystem.getPath("test2");
                    Files.createFile(test);
                    return PartOutput.from(test);
                })
                .onRequestComplete(context -> context.getUserObject(HttpServletResponse.class).setStatus(HttpServletResponse.SC_OK));
    }

    @Override
    public PartOutput onPartBegin(final UploadContext context, final ByteBuffer buffer) {
        return PartOutput.from(new NullChannel());
    }

    @Override
    public void onPartEnd(final UploadContext context) {
        // No-op
    }

    @Override
    public void onRequestComplete(final UploadContext context) {
        // No-op
    }

    @Override
    public void onError(final UploadContext context, final Throwable throwable) {
        // No-op
    }
}
