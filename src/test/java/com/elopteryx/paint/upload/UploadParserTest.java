package com.elopteryx.paint.upload;

import com.elopteryx.paint.upload.impl.AsyncUploadParser;
import com.elopteryx.paint.upload.impl.BlockingUploadParser;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.*;
import static com.elopteryx.paint.upload.FunctionSupplier.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class UploadParserTest {

    @Test
    public void createAsyncParser() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.isAsyncSupported()).thenReturn(true);

        UploadParser asyncParser = UploadParser.newParser(request, response);
        assertThat(asyncParser, instanceOf(AsyncUploadParser.class));
    }

    @Test
    public void createBlockingParser() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();
        
        when(request.isAsyncSupported()).thenReturn(false);

        UploadParser blockingParser = UploadParser.newParser(request, response);
        assertThat(blockingParser, instanceOf(BlockingUploadParser.class));
    }

    @Test
    public void useTheFullApi() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.startAsync()).thenReturn(new MockAsyncContext(request, response));

        UploadParser.newParser(request, response)
                .onPartBegin(partValidator())
                .onPartEnd(partExecutor())
                .onRequestComplete(requestExecutor())
                .onError(errorExecutor())
                .sizeThreshold(1024 * 1024 * 10)
                .maxPartSize(1024 * 1024 * 50)
                .maxRequestSize(1024 * 1024 * 50)
                .setup();
    }

    private HttpServletRequest newRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("multipart/");
        when(request.getInputStream()).thenReturn(new MockServletInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        }));
        when(request.isAsyncSupported()).thenReturn(true);

        return request;
    }

    private HttpServletResponse newResponse() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(response.getStatus()).thenReturn(200);
        
        return response;
    }

}
