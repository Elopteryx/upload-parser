package com.github.elopteryx.upload.internal;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.github.elopteryx.upload.OnError;
import com.github.elopteryx.upload.OnPartBegin;
import com.github.elopteryx.upload.OnPartEnd;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.errors.MultipartException;

import com.github.elopteryx.upload.util.Servlets;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BlockingUploadParserTest implements OnPartBegin, OnPartEnd, OnError {
    
    private final List<ByteArrayOutputStream> strings = new ArrayList<>();

    @Test
    public void this_should_end_with_multipart_exception() throws Exception {
        HttpServletRequest request = Servlets.newRequest();
        HttpServletResponse response = Servlets.newResponse();

        when(request.isAsyncSupported()).thenReturn(false);
        when(request.getHeader(Headers.CONTENT_TYPE)).thenReturn("multipart/form-data; boundary=----1234");

        UploadParser.newParser()
                .onPartBegin(this)
                .onPartEnd(this)
                .onError(this)
                .userObject(response)
                .doBlockingParse(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void this_should_end_with_illegal_argument_exception() throws Exception {
        HttpServletRequest request = Servlets.newRequest();

        when(request.isAsyncSupported()).thenReturn(false);
        when(request.getHeader(Headers.CONTENT_TYPE)).thenReturn("multipart/form-data;");

        UploadParser.newParser().doBlockingParse(request);
    }

    @Override
    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        strings.add(baos);
        return PartOutput.from(baos);
    }

    @Override
    public void onPartEnd(UploadContext context) throws IOException {
        System.out.println(strings.get(strings.size() - 1).toString());
    }

    @Override
    public void onError(UploadContext context, Throwable throwable) {
        assertThat(throwable, instanceOf(MultipartException.class));
    }
}
