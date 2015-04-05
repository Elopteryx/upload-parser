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

package com.elopteryx.paint.upload.rs.internal;

import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.internal.PartStreamImpl;
import com.elopteryx.paint.upload.rs.Part;

import javax.annotation.Nonnull;

/**
 * Default implementation of {@link com.elopteryx.paint.upload.rs.Part}.
 */
public class PartImpl extends PartStreamImpl implements Part {

    public PartImpl(PartStreamImpl partStream) {
        super(partStream.getSubmittedFileName(), partStream.getName(), partStream.getHeadersObject());
        this.output = partStream.getOutput();
    }

    @Override
    public long getSize() {
        return getKnownSize();
    }

    @Nonnull
    @Override
    public PartOutput getOutPut() {
        return output;
    }
}
