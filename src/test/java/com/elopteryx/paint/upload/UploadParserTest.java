package com.elopteryx.paint.upload;

import com.elopteryx.paint.upload.impl.AsyncUploadParser;
import com.elopteryx.paint.upload.impl.BlockingUploadParser;
import com.elopteryx.paint.upload.util.MockAsyncContext;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;
import static com.elopteryx.paint.upload.util.FunctionSupplier.*;
import static com.elopteryx.paint.upload.util.Servlets.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class UploadParserTest {

    @Test(expected = ServletException.class)
    public void valid_and_invalid_content_type() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.getContentType()).thenReturn("multipart/");
        assertTrue(UploadParser.isMultipart(request));

        when(request.getContentType()).thenReturn("text/plain;charset=UTF-8");
        assertFalse(UploadParser.isMultipart(request));
        UploadParser.newParser(request, response);
    }

    @Test
    public void create_async_parser() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.isAsyncSupported()).thenReturn(true);

        UploadParser asyncParser = UploadParser.newParser(request, response);
        assertThat(asyncParser, instanceOf(AsyncUploadParser.class));
    }

    @Test
    public void create_blocking_parser() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();
        
        when(request.isAsyncSupported()).thenReturn(false);

        UploadParser blockingParser = UploadParser.newParser(request, response);
        assertThat(blockingParser, instanceOf(BlockingUploadParser.class));
    }

    @Test
    public void use_the_full_api() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.startAsync()).thenReturn(new MockAsyncContext(request, response));

        UploadParser.newParser(request, response)
                .onPartBegin(partBeginCallback())
                .onPartEnd(partEndCallback())
                .onRequestComplete(requestCallback())
                .onError(errorCallback())
                .sizeThreshold(1024 * 1024 * 10)
                .maxPartSize(1024 * 1024 * 50)
                .maxRequestSize(1024 * 1024 * 50)
                .setup();
    }

}
