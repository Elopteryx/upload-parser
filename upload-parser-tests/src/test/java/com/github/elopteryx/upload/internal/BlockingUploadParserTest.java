package com.github.elopteryx.upload.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.elopteryx.upload.OnError;
import com.github.elopteryx.upload.OnPartBegin;
import com.github.elopteryx.upload.OnPartEnd;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.errors.MultipartException;
import com.github.elopteryx.upload.util.Servlets;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BlockingUploadParserTest implements OnPartBegin, OnPartEnd, OnError {
    
    private final List<ByteArrayOutputStream> strings = new ArrayList<>();

    @Test
    void this_should_end_with_multipart_exception() throws Exception {
        final var request = Servlets.newRequest();
        final var response = Servlets.newResponse();

        when(request.isAsyncSupported()).thenReturn(false);
        when(request.getHeader(Headers.CONTENT_TYPE)).thenReturn("multipart/form-data; boundary=----1234");

        UploadParser.newParser()
                .onPartBegin(this)
                .onPartEnd(this)
                .onError(this)
                .userObject(response)
                .doBlockingParse(request);
    }

    @Test
    void this_should_end_with_illegal_argument_exception() throws Exception {
        final var request = Servlets.newRequest();

        when(request.isAsyncSupported()).thenReturn(false);
        when(request.getHeader(Headers.CONTENT_TYPE)).thenReturn("multipart/form-data;");

        assertThrows(IllegalArgumentException.class, () -> UploadParser.newParser().doBlockingParse(request));
    }

    @Override
    public PartOutput onPartBegin(final UploadContext context, final ByteBuffer buffer) {
        final var baos = new ByteArrayOutputStream();
        strings.add(baos);
        return PartOutput.from(baos);
    }

    @Override
    public void onPartEnd(final UploadContext context) {
        assertFalse(strings.isEmpty());
    }

    @Override
    public void onError(final UploadContext context, final Throwable throwable) {
        assertTrue(throwable instanceof MultipartException);
    }
}
