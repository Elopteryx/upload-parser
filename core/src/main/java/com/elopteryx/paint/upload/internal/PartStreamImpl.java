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

package com.elopteryx.paint.upload.internal;

import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.PartStream;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Default implementation of {@link com.elopteryx.paint.upload.PartStream}.
 */
public class PartStreamImpl implements PartStream {

    /**
     * The content type of the part.
     */
    private final String contentType;
    /**
     * The file name of the part.
     */
    private final String fileName;
    /**
     * The field name of the part.
     */
    private final String fieldName;
    /**
     * Whether the part is a file field.
     */
    private final boolean fileField;
    /**
     * The headers, if any.
     */
    private final PartStreamHeaders headers;
    /**
     * The size of the part, updated on each read.
     */
    private long size;
    /**
     * Boolean flag storing whether the part is
     * completely uploaded.
     */
    private boolean finished;
    /**
     * The output object supplied by the caller. Not used here, but for
     * the Jax-Rs module it has to be made available.
     */
    protected PartOutput output;

    /**
     * Creates a new instance.
     * @param fileName The file name.
     * @param fieldName The form field name.
     * @param headers The object containing the headers
     */
    public PartStreamImpl(String fileName, String fieldName, PartStreamHeaders headers) {
        this.fileName = fileName;
        this.fieldName = fieldName;
        this.contentType = headers.getHeader(PartStreamHeaders.CONTENT_TYPE);
        this.fileField = fileName != null;
        this.headers = headers;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getName() {
        return fieldName;
    }

    @Override
    public long getKnownSize() {
        return size;
    }

    @Override
    public String getSubmittedFileName() {
        return checkFileName(fileName);
    }

    @Override
    public boolean isFile() {
        return fileField;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getHeader(String name) {
        return headers.getHeader(name);
    }

    @Override
    @Nonnull
    public Collection<String> getHeaderNames() {
        return headers.getHeaderNames();
    }

    @Override
    @Nonnull
    public Collection<String> getHeaders(String name) {
        return headers.getHeaders(name);
    }

    public PartStreamHeaders getHeadersObject() {
        return headers;
    }

    void setSize(long size) {
        this.size = size;
    }

    void markAsFinished() {
        this.finished = true;
    }

    public PartOutput getOutput() {
        return output;
    }

    void setOutput(PartOutput output) {
        this.output = output;
    }

    private String checkFileName(String fileName) {
        if (fileName != null && fileName.indexOf('\u0000') != -1) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fileName.length(); i++) {
                char character = fileName.charAt(i);
                switch (character) {
                    case 0:
                        sb.append("\\0");
                        break;
                    default:
                        sb.append(character);
                        break;
                }
            }
            throw new IllegalArgumentException(fileName + " Invalid file name: " + sb);
        }
        return fileName;
    }

}
