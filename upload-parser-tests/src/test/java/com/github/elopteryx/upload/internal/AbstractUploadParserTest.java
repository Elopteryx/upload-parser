package com.github.elopteryx.upload.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.elopteryx.upload.errors.PartSizeException;
import com.github.elopteryx.upload.errors.RequestSizeException;

import com.github.elopteryx.upload.util.Servlets;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

class AbstractUploadParserTest {

    private static final long size = 1024 * 1024 * 100L;
    private static final long smallSize = 1024;

    private AbstractUploadParser runSetupForSize(long requestSize, long allowedRequestSize, long allowedPartSize) throws Exception {
        HttpServletRequest request = Servlets.newRequest();

        when(request.getContentLengthLong()).thenReturn(requestSize);

        AbstractUploadParser parser = new AsyncUploadParser(request);
        parser.setMaxPartSize(allowedPartSize);
        parser.setMaxRequestSize(allowedRequestSize);
        ((AsyncUploadParser)parser).setupAsyncParse();
        return parser;
    }

    @Test
    void setup_should_work_if_lesser() throws Exception {
        runSetupForSize(size - 1, size, -1);
    }

    @Test
    void setup_should_work_if_equals() throws Exception {
        runSetupForSize(size, size, -1);
    }

    @Test
    void setup_should_throw_size_exception_if_greater() {
        assertThrows(RequestSizeException.class, () -> runSetupForSize(size + 1, size, -1));
    }

    @Test
    void parser_should_throw_exception_for_request_size() {
        final RequestSizeException exception = assertThrows(RequestSizeException.class, () -> {
            AbstractUploadParser parser = runSetupForSize(0, smallSize, -1);
            for (int i = 0; i < 11; i++) {
                parser.checkRequestSize(100);
            }
        });
        assertEquals(exception.getPermittedSize(), smallSize);
        assertTrue(exception.getActualSize() > smallSize);
    }

    @Test
    void parser_should_throw_exception_for_part_size() {
        final PartSizeException exception = assertThrows(PartSizeException.class, () -> {
            AbstractUploadParser parser = runSetupForSize(0, -1, smallSize);
            for (int i = 0; i < 11; i++) {
                parser.checkPartSize(100);
            }
        });
        assertEquals(exception.getPermittedSize(), smallSize);
        assertTrue(exception.getActualSize() > smallSize);
    }
    
}
