package com.elopteryx.paint.upload.rs;

import static org.mockito.Mockito.mock;

import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.AsyncResponse;

public class UploadResponseTest {

    @Test
    public void create_upload_response() {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        UploadResponse.from(asyncResponse);

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        UploadResponse.from(servletResponse);
    }
}
