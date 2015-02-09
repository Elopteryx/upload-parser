package com.elopteryx.paint.upload;

import com.elopteryx.paint.upload.impl.AsyncUploadParser;
import com.elopteryx.paint.upload.impl.BlockingUploadParser;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;
import static com.elopteryx.paint.upload.FunctionSupplier.*;
import static com.elopteryx.paint.upload.Servlets.*;
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
