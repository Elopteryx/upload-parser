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

package com.github.elopteryx.upload.rs.internal;

import com.github.elopteryx.upload.rs.MultiPart;
import com.github.elopteryx.upload.rs.Part;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link MultiPart}.
 */
public class MultiPartImpl implements MultiPart {

    /**
     * The list of the received parts.
     */
    private final List<Part> parts;
    /**
     * The request size.
     */
    private final long size;
    /**
     * The map of the HTTP headers.
     */
    private MultivaluedMap<String, String> headers;

    MultiPartImpl(List<Part> parts, long size) {
        this.parts = parts != null ? Collections.unmodifiableList(parts) : Collections.emptyList();
        this.size = size;
    }

    @Nonnull
    @Override
    public List<Part> getParts() {
        return parts;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Nonnull
    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }
}
