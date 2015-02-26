package com.elopteryx.paint.upload.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Servlets {

    public static HttpServletRequest newRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("multipart/");
        when(request.getContentLengthLong()).thenReturn(1024 * 1024L);
        when(request.getInputStream()).thenReturn(new MockServletInputStream());
        when(request.isAsyncSupported()).thenReturn(true);

        return request;
    }

    public static HttpServletResponse newResponse() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(response.getStatus()).thenReturn(200);

        return response;
    }
}
