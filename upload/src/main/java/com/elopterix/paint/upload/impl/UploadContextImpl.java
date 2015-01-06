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
package com.elopterix.paint.upload.impl;

import com.elopterix.paint.upload.PartStream;
import com.elopterix.paint.upload.UploadContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Meta data holder of an upload operation. Its fields are
 * managed by the handler objects.
 */
class UploadContextImpl implements UploadContext {

    /**
     * The request containing the bytes of the file. It's always a multipart, POST request.
     */
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private PartStream currentItem;

    private boolean buffering = true;

    /* The total number for the bytes read for the current item. */
    private int partBytesRead;


    UploadContextImpl(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    //Public API

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
    public PartStream getCurrentItem() {
        return currentItem;
    }


    //Internal methods

    void setCurrentItem(PartStream currentItem) {
        this.currentItem = currentItem;
    }

    void reset(PartStream newItem) {
        currentItem = newItem;
        buffering = true;
        partBytesRead = 0;
    }

    boolean isBuffering() {
        return buffering;
    }

    public void finishBuffering() {
        this.buffering = false;
    }

    int getPartBytesRead() {
        return partBytesRead;
    }

    int incrementAndGetPartBytes(int additional) {
        partBytesRead += additional;
        return partBytesRead;
    }

}
