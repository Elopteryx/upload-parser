package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.UploadParser;
import com.elopteryx.paint.upload.errors.PartSizeException;
import com.elopteryx.paint.upload.errors.RequestSizeException;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;
import static com.elopteryx.paint.upload.Servlets.*;
import static com.elopteryx.paint.upload.FunctionSupplier.*;
import static org.junit.Assert.*;

public class UploadParserImplTest {

    private static final long size = 1024 * 1024 * 100L;
    private static final long smallSize = 1024;

    private UploadParserImpl runSetupForSize(long requestSize, long allowedRequestSize, long allowedPartSize) throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.getContentLengthLong()).thenReturn(requestSize);

        UploadParser parser = UploadParser.newParser(request, response)
                .onPartBegin(partValidator())
                .onPartEnd(partExecutor())
                .maxPartSize(allowedPartSize)
                .maxRequestSize(allowedRequestSize);
        parser.setup();
        return (UploadParserImpl)parser;
    }

    @Test
    public void setup_should_work_if_lesser() throws Exception {
        runSetupForSize(size - 1, size, -1);
    }

    @Test
    public void setup_should_work_if_equals() throws Exception {
        runSetupForSize(size, size, -1);
    }

    @Test(expected = RequestSizeException.class)
    public void setup_should_throw_size_exception_if_greater() throws Exception {
        runSetupForSize(size + 1, size, -1);
    }

    @Test(expected = RequestSizeException.class)
    public void parser_should_throw_exception_for_request_size() throws Exception {
        UploadParserImpl parser = runSetupForSize(0, smallSize, -1);
        try {
            for(int i = 0; i < 11; i++)
                parser.checkRequestSize(100);
        } catch (RequestSizeException e) {
            assertEquals(e.getPermittedSize(), smallSize);
            assertTrue(e.getActualSize() > smallSize);
            throw e;
        }
    }

    @Test(expected = PartSizeException.class)
    public void parser_should_throw_exception_for_part_size() throws Exception {
        UploadParserImpl parser = runSetupForSize(0, -1, smallSize);
        try {
            for(int i = 0; i < 11; i++)
                parser.checkPartSize(100);
        } catch (PartSizeException e) {
            assertEquals(e.getPermittedSize(), smallSize);
            assertTrue(e.getActualSize() > smallSize);
            throw e;
        }
    }
    
}
