package com.elopterix.paint.upload.impl;

import com.elopterix.paint.upload.PartStream;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;

/**
 * Default implementation of {@link com.elopterix.paint.upload.PartStream}.
 */
class PartStreamImpl implements PartStream {

    /**
     * The file items content type.
     */
    private final String contentType;

    /**
     * The file items field name.
     */
    private final String fieldName;

    /**
     * The file items file name.
     */
    private final String name;

    /**
     * Whether the file item is a form field.
     */
    private final boolean fileField;

    /**
     * The file items input stream.
     */
    private final ReadableByteChannel channel;

    /**
     * The headers, if any.
     */
    private PartStreamHeaders headers;

    /**
     * Creates a new instance.
     *
     * @param pName        The items file name, or null.
     * @param pFieldName   The items field name.
     * @param pContentType The items content type, or null.
     * @param pFileField   Whether the item is a form field.
     */
    PartStreamImpl(String pName, String pFieldName, String pContentType, boolean pFileField,
                   ReadableByteChannel channel, PartStreamHeaders headers) {
        name = pName;
        fieldName = pFieldName;
        contentType = pContentType;
        fileField = pFileField;
        this.channel = channel;
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

    /**
     * Returns an input stream, which may be used to
     * read the items contents.
     *
     * @return Opened input stream.
     * @throws java.io.IOException An I/O error occurred.
     */
    public ReadableByteChannel getChannel() throws IOException {
        if (!channel.isOpen()) {
            throw new IOException("The channel is already closed!");
        }
        return channel;
    }

    /**
     * Closes the file item.
     *
     * @throws IOException An I/O error occurred.
     */
    void close() throws IOException {
        channel.close();
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
