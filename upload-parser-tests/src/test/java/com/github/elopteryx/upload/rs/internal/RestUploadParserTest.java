package com.github.elopteryx.upload.rs.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.github.elopteryx.upload.errors.MultipartException;
import com.github.elopteryx.upload.internal.Headers;
import com.github.elopteryx.upload.util.Servlets;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

class RestUploadParserTest {


    @Test
    void this_should_end_with_multipart_exception() throws Exception {
        HttpServletRequest request = Servlets.newRequest();

        when(request.isAsyncSupported()).thenReturn(false);
        when(request.getHeader(Headers.CONTENT_TYPE)).thenReturn("multipart/form-data; boundary=----1234");

        RestUploadParser parser = new RestUploadParser();
        long requestSize = 1024;
        String mimeType = "multipart/form-data; boundary=----1234";
        String encodingHeader = "UTF-8";
        assertThrows(MultipartException.class, () -> {
            parser.doBlockingParse(requestSize, mimeType, encodingHeader, request.getInputStream());
        });

    }

    @Test
    void this_should_end_with_illegal_argument_exception() throws Exception {
        HttpServletRequest request = Servlets.newRequest();
        RestUploadParser parser = new RestUploadParser();

        long requestSize = 1024;
        String mimeType = "multipart/form-data; boundary;";
        String encodingHeader = "UTF-8";
        assertThrows(IllegalArgumentException.class, () -> {
            parser.doBlockingParse(requestSize, mimeType, encodingHeader, request.getInputStream());
        });
    }
}
