package com.elopteryx.paint.upload.impl;

import static com.elopteryx.paint.upload.util.Servlets.newRequest;
import static org.mockito.Mockito.when;

import com.elopteryx.paint.upload.UploadParser;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

public class AsyncUploadParserTest {

    @Test(expected = IllegalStateException.class)
    public void this_should_end_with_illegal_state_exception() throws Exception {
        HttpServletRequest request = newRequest();

        when(request.isAsyncSupported()).thenReturn(false);

        UploadParser.newParser().setupAsyncParse(request);
    }
}
