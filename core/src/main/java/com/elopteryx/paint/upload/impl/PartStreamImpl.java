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

import com.elopteryx.paint.upload.PartStream;

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
     * Creates a new instance.
     * @param fileName The file name.
     * @param fieldName The form field name.
     * @param headers The object containing the headers
     */
    PartStreamImpl(String fileName, String fieldName, PartStreamHeaders headers) {
        this.fileName = fileName;
        this.fieldName = fieldName;
        this.contentType = headers.getHeader(PartStreamHeaders.CONTENT_TYPE);
        this.fileField = fileName != null;
        this.headers = headers;
    }

    /**
     * Returns the content type of the part, or null.
     *
     * @return Content type, if known, or null.
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the field name of the part.
     *
     * @return Field name.
     */
    @Override
    public String getName() {
        return fieldName;
    }

    /**
     * Returns the known size of the part.
     *
     * @return Part size.
     */
    @Override
    public long getKnownSize() {
        return size;
    }

    /**
     * Returns the file name of the part. Returns null
     * if it's a normal form field.
     *
     * @return File name, if known, or null.
     */
    @Override
    public String getSubmittedFileName() {
        return checkFileName(fileName);
    }

    /**
     * Returns whether the part is a form field.
     *
     * @return True, if the part is a form field,
     * otherwise false.
     */
    @Override
    public boolean isFile() {
        return fileField;
    }

    @Override
    public String getHeader(String name) {
        return headers.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.getHeaderNames();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getHeaders(name);
    }

    void setSize(long size) {
        this.size = size;
    }

    private String checkFileName(String fileName) {
        if (fileName != null && fileName.indexOf('\u0000') != -1) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fileName.length(); i++) {
                char c = fileName.charAt(i);
                switch (c) {
                    case 0:
                        sb.append("\\0");
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            throw new IllegalArgumentException(fileName + " Invalid file name: " + sb);
        }
        return fileName;
    }

}
