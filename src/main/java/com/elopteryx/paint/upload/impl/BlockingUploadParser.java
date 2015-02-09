package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.errors.MultipartException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The blocking implementation of the parser. If the calling servlet does not
 * support async mode, then the created upload parser will be an instance of
 * this class.
 *
 * This class is only public to serve as an entry point in the implementation package, users
 * should not need to directly depend on this class.
 */
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
            if(requestCallback != null)
                requestCallback.onRequestComplete(context);
        } catch (Exception e) {
            if(errorCallback != null)
                errorCallback.onError(context, e);
        }
    }
}
