package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.OnError;
import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.UploadParser;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import static com.elopteryx.paint.upload.util.Servlets.*;
import static org.mockito.Mockito.when;

public class BlockingUploadParserTest implements OnPartBegin, OnPartEnd, OnError {
    
    private List<ByteArrayOutputStream> strings = new ArrayList<>();
    
    @Test
    public void the_whole_parsing_should_work() throws Exception {
        HttpServletRequest request = newRequest();
        HttpServletResponse response = newResponse();

        when(request.isAsyncSupported()).thenReturn(false);
        when(request.getHeader(PartStreamHeaders.CONTENT_TYPE)).thenReturn("multipart/form-data; boundary=----1234");

        UploadParser parser = UploadParser.newParser(request, response)
                .onPartBegin(this)
                .onPartEnd(this)
                .onError(this);
        BlockingUploadParser blockingParser = (BlockingUploadParser)parser;
        blockingParser.setup();
    }

    @Override
    public WritableByteChannel onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        strings.add(baos);
        return Channels.newChannel(baos);
    }

    @Override
    public void onPartEnd(UploadContext context) throws IOException {
        System.out.println(strings.get(strings.size() - 1).toString());
    }

    @Override
    public void onError(UploadContext context, Throwable throwable) {
        throwable.printStackTrace();
    }
}
