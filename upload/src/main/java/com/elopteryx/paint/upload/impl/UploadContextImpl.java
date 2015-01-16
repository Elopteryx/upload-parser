/*
 * Copyright (C) 2015- Adam Forgacs
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

import com.elopteryx.paint.upload.PartStream;
import com.elopteryx.paint.upload.UploadContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Meta data holder of an upload operation. Its fields are
 * managed by the handler objects.
 */
class UploadContextImpl implements UploadContext {

    /**
     * The request containing the bytes of the file. It's always a multipart, POST request.
     */
    private final HttpServletRequest request;
    /**
     * The response object. Only used by the callers, not necessary for these classes. 
     */
    private final HttpServletResponse response;
    /**
     * The currently processed item. 
     */
    private PartStreamImpl currentPart;
    /**
     * The list of the already processed items.
     */
    private List<PartStream> partStreams = new ArrayList<>();
    /**
     * Determines whether the current item is buffering, that is, should new bytes be
     * stored in memory or written out the channel. It is set to false after the
     * validator function is called. 
     */
    private boolean buffering = true;
    /**
     * The total number for the bytes read for the current part.
     */
    private int partBytesRead;

    UploadContextImpl(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    @Nonnull
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    @Nonnull
    public HttpServletResponse getResponse() {
        return response;
    }

    @Override
    @Nonnull
    public PartStream getCurrentPart() {
        return currentPart;
    }

    @Nonnull
    @Override
    public List<PartStream> getPartStreams() {
        return Collections.unmodifiableList(partStreams);
    }

    void reset(PartStreamImpl newPart) {
        currentPart = newPart;
        buffering = true;
        partBytesRead = 0;
        partStreams.add(newPart);
    }

    boolean isBuffering() {
        return buffering;
    }

    void finishBuffering() {
        this.buffering = false;
    }

    void updatePartBytesRead() {
        currentPart.setSize(partBytesRead);
    }

    int incrementAndGetPartBytes(int additional) {
        partBytesRead += additional;
        return partBytesRead;
    }
}
