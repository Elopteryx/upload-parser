package com.github.elopteryx.upload.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.elopteryx.upload.errors.PartSizeException;
import com.github.elopteryx.upload.errors.RequestSizeException;
import com.github.elopteryx.upload.util.Servlets;
import org.junit.jupiter.api.Test;

class AbstractUploadParserTest {

    private static final long SIZE = 1024 * 1024 * 100L;
    private static final long SMALL_SIZE = 1024;

    private AbstractUploadParser runSetupForSize(final long requestSize, final long allowedRequestSize, final long allowedPartSize) throws Exception {
        final var request = Servlets.newRequest();

        when(request.getContentLengthLong()).thenReturn(requestSize);

        final var parser = new AsyncUploadParser(request);
        parser.setMaxPartSize(allowedPartSize);
        parser.setMaxRequestSize(allowedRequestSize);
        parser.setupAsyncParse();
        return parser;
    }

    @Test
    void setup_should_work_if_lesser() throws Exception {
        runSetupForSize(SIZE - 1, SIZE, -1);
    }

    @Test
    void setup_should_work_if_equals() throws Exception {
        runSetupForSize(SIZE, SIZE, -1);
    }

    @Test
    void setup_should_throw_size_exception_if_greater() {
        assertThrows(RequestSizeException.class, () -> runSetupForSize(SIZE + 1, SIZE, -1));
    }

    @Test
    void parser_should_throw_exception_for_request_size() {
        final var exception = assertThrows(RequestSizeException.class, () -> {
            final var parser = runSetupForSize(0, SMALL_SIZE, -1);
            for (var i = 0; i < 11; i++) {
                parser.checkRequestSize(100);
            }
        });
        assertEquals(exception.getPermittedSize(), SMALL_SIZE);
        assertTrue(exception.getActualSize() > SMALL_SIZE);
    }

    @Test
    void parser_should_throw_exception_for_part_size() {
        final var exception = assertThrows(PartSizeException.class, () -> {
            final var parser = runSetupForSize(0, -1, SMALL_SIZE);
            for (var i = 0; i < 11; i++) {
                parser.checkPartSize(100);
            }
        });
        assertEquals(exception.getPermittedSize(), SMALL_SIZE);
        assertTrue(exception.getActualSize() > SMALL_SIZE);
    }
    
}
