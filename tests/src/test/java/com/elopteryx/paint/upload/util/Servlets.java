package com.elopteryx.paint.upload.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Servlets {

    /**
     * Creates a new mock servlet request.
     * @return The mocked request.
     * @throws Exception If an error occurred
     */
    public static HttpServletRequest newRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("multipart/");
        when(request.getContentLengthLong()).thenReturn(1024 * 1024L);
        when(request.getInputStream()).thenReturn(new MockServletInputStream());
        when(request.isAsyncSupported()).thenReturn(true);

        return request;
    }

    /**
     * Creates a new mock servlet response.
     * @return The mocked response.
     */
    public static HttpServletResponse newResponse() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(response.getStatus()).thenReturn(200);

        return response;
    }
}
