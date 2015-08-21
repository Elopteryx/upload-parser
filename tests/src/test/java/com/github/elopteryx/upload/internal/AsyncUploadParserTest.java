package com.github.elopteryx.upload.internal;

import static org.mockito.Mockito.when;

import com.github.elopteryx.upload.UploadParser;
import com.github.elopteryx.upload.errors.MultipartException;
import com.github.elopteryx.upload.util.MockServletInputStream;

import com.github.elopteryx.upload.util.Servlets;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

public class AsyncUploadParserTest {

    @Test(expected = MultipartException.class)
    public void this_should_end_with_multipart_exception() throws Exception {
        HttpServletRequest request = Servlets.newRequest();

        when(request.isAsyncSupported()).thenReturn(true);
        when(request.getHeader(Headers.CONTENT_TYPE)).thenReturn("multipart/form-data; boundary=----1234");

        UploadParser.newParser().setupAsyncParse(request);
        MockServletInputStream servletInputStream = (MockServletInputStream)request.getInputStream();
        servletInputStream.onDataAvailable();
    }

    @Test(expected = IllegalStateException.class)
    public void this_should_end_with_illegal_state_exception() throws Exception {
        HttpServletRequest request = Servlets.newRequest();

        when(request.isAsyncSupported()).thenReturn(false);

        UploadParser.newParser().setupAsyncParse(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void this_should_end_with_illegal_argument_exception() throws Exception {
        HttpServletRequest request = Servlets.newRequest();

        when(request.isAsyncSupported()).thenReturn(true);
        when(request.getHeader(Headers.CONTENT_TYPE)).thenReturn("multipart/form-data; boundary;");

        UploadParser.newParser().setupAsyncParse(request);
    }
}
