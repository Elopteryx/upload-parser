package com.elopteryx.paint.upload.internal;

import static com.elopteryx.paint.upload.util.Servlets.newRequest;
import static org.mockito.Mockito.when;

import com.elopteryx.paint.upload.UploadParser;
import com.elopteryx.paint.upload.errors.MultipartException;
import com.elopteryx.paint.upload.util.MockServletInputStream;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

public class AsyncUploadParserTest {

    @Test(expected = MultipartException.class)
    public void this_should_end_with_multipart_exception() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.isAsyncSupported()).thenReturn(true);
        when(request.getHeader(PartStreamHeaders.CONTENT_TYPE)).thenReturn("multipart/form-data; boundary=----1234");

        UploadParser.newParser().setupAsyncParse(request);
        MockServletInputStream servletInputStream = (MockServletInputStream)request.getInputStream();
        servletInputStream.onDataAvailable();
    }

    @Test(expected = IllegalStateException.class)
    public void this_should_end_with_illegal_state_exception() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.isAsyncSupported()).thenReturn(false);

        UploadParser.newParser().setupAsyncParse(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void this_should_end_with_illegal_argument_exception() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.isAsyncSupported()).thenReturn(true);
        when(request.getHeader(PartStreamHeaders.CONTENT_TYPE)).thenReturn("multipart/form-data; boundary;");

        UploadParser.newParser().setupAsyncParse(request);
    }
}
