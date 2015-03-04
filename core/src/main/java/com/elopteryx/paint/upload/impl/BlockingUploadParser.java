/*
 * Copyright (C) 2015 Adam Forgacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.errors.MultipartException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The blocking implementation of the parser. This parser can be used to perform a
 * blocking parse, whether the servlet supports async mode or not.
 */
public class BlockingUploadParser extends UploadParser<BlockingUploadParser> {

    public BlockingUploadParser(HttpServletRequest request) {
        super(request);
    }

    /**
     * The parser begins parsing the request stream. This is a blocking method,
     * the method will not finish until the upload process finished, either
     * successfully or not.
     * @return The upload context
     * @throws IOException If an error occurred with the servlet stream
     */
    public UploadContext parse() throws IOException {
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
        return context;
    }
}
