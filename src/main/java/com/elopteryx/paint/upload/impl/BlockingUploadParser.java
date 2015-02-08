package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.errors.MultipartException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BlockingUploadParser extends UploadParserImpl {

    public BlockingUploadParser(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public void setup() throws IOException {
        super.setup();
        try {
            while(true) {
                int c = servletInputStream.read(buf);
                if (c == -1) {
                    if (!parseState.isComplete())
                        throw new MultipartException();
                    else
                        break;
                } else if(c > 0) {
                    checkRequestSize(c);
                    parseState.parse(ByteBuffer.wrap(buf, 0, c));
                }
            }
            if(completeExecutor != null)
                completeExecutor.accept(context);
        } catch (Exception e) {
            if(errorExecutor != null)
                errorExecutor.accept(context, e);
        }
    }
}
