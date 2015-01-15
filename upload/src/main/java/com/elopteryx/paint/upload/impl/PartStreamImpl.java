package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.PartStream;

import java.util.Collection;

/**
 * Default implementation of {@link com.elopteryx.paint.upload.PartStream}.
 */
class PartStreamImpl implements PartStream {

    /**
     * The content type of the part.
     */
    private final String contentType;
    /**
     * The field name of the part.
     */
    private final String fieldName;
    /**
     * The file items file name of the part.
     */
    private final String name;
    /**
     * Whether the part is a file field.
     */
    private final boolean fileField;
    /**
     * The headers, if any.
     */
    private final PartStreamHeaders headers;

    /**
     * Creates a new instance.
     *
     * @param name        The items file name, or null.
     * @param fieldName   The items field name.
     * @param contentType The items content type, or null.
     */
    PartStreamImpl(String name, String fieldName, String contentType, PartStreamHeaders headers) {
        this.name = name;
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.fileField = name != null;
        this.headers = headers;
    }

    /**
     * Returns the items content type, or null.
     *
     * @return Content type, if known, or null.
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the items field name.
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
    public long getSize() {
        return -1;
    }

    /**
     * Returns the items file name.
     *
     * @return File name, if known, or null.
     */
    @Override
    public String getSubmittedFileName() {
        return checkFileName(name);
    }

    /**
     * Returns, whether this is a form field.
     *
     * @return True, if the item is a form field,
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
