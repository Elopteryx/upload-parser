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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * The iterator.
 */
class UploadIterator {

    /**
     * HTTP content type header name.
     */
    static final String CONTENT_TYPE = "Content-type";
    /**
     * HTTP content disposition header name.
     */
    private static final String CONTENT_DISPOSITION = "Content-disposition";
    /**
     * Content-disposition value for form data.
     */
    private static final String FORM_DATA = "form-data";
    /**
     * Content-disposition value for file attachment.
     */
    private static final String ATTACHMENT = "attachment";
    /**
     * HTTP content type header for multiple uploads.
     */
    private static final String MULTIPART_MIXED = "multipart/mixed";
    /**
     * The multi part stream to process.
     */
    private final MultipartChannel multi;
    /**
     * The boundary, which separates the various parts.
     */
    private final byte[] boundary;
    /**
     * The item, which we currently process.
     */
    private PartStreamImpl currentItem;
    /**
     * The current items field name.
     */
    private String currentFieldName;
    /**
     * Whether the current item may still be read.
     */
    private boolean itemValid;
    /**
     * Whether we have seen the end of the file.
     */
    private boolean eof;
    /**
     * Parameter parser instance. 
     */
    private final ParameterParser parser = new ParameterParser();

    /**
     * Creates a new instance.
     *
     * @param request The request context.
     */
    UploadIterator(HttpServletRequest request) {
        try {
            boundary = getBoundary(request.getContentType());
            if (boundary == null) {
                throw new IllegalArgumentException("The request was rejected because no multipart boundary was found");
            }
            try {
                multi = new MultipartChannel(request.getInputStream(), boundary);
            } catch (IllegalArgumentException iae) {
                throw new RuntimeException("The boundary specified in the " + CONTENT_TYPE + " header is too long", iae);
            }
            multi.setHeaderEncoding(request.getCharacterEncoding());
            findNextItem();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long getBytesRead() {
        return multi.getBytesRead();
    }

    /**
     * Retrieves the boundary from the <code>Content-type</code> header.
     *
     * @param contentType The value of the content type header from which to
     *                    extract the boundary value.
     * @return The boundary, as a byte array.
     */
    byte[] getBoundary(String contentType) {
        Map<String, String> params = parser.parse(contentType, ';', ',');
        String boundaryStr = params.get("boundary");
        return boundaryStr == null ? null : boundaryStr.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Retrieves the file name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers The HTTP headers object.
     * @return The file name for the current <code>encapsulation</code>.
     */
    String getFileName(PartStreamHeaders headers) {
        String contentDisposition = headers.getHeader(CONTENT_DISPOSITION);
        String fileName = null;
        if (contentDisposition != null) {
            String cdl = contentDisposition.toLowerCase(Locale.ENGLISH);
            if (cdl.startsWith(FORM_DATA) || cdl.startsWith(ATTACHMENT)) {
                // Parameter parser can handle null input
                Map<String, String> params = parser.parse(contentDisposition, ';');
                if (params.containsKey("filename")) {
                    fileName = params.get("filename");
                    if (fileName != null) {
                        fileName = fileName.trim();
                    } else {
                        // Even if there is no value, the parameter is present,
                        // so we return an empty file name rather than no file
                        // name.
                        fileName = "";
                    }
                }
            }
        }
        return fileName;
    }

    /**
     * Retrieves the field name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers A <code>Map</code> containing the HTTP request headers.
     * @return The field name for the current <code>encapsulation</code>.
     */
    String getFieldName(PartStreamHeaders headers) {
        String contentDisposition = headers.getHeader(CONTENT_DISPOSITION);
        String fieldName = null;
        if (contentDisposition != null && contentDisposition.toLowerCase(Locale.ENGLISH).startsWith(FORM_DATA)) {
            Map<String, String> params = parser.parse(contentDisposition, ';');
            fieldName = params.get("name");
            if (fieldName != null) {
                fieldName = fieldName.trim();
            }
        }
        return fieldName;
    }

    /**
     * <p> Parses the <code>header-part</code> and returns as key/value
     * pairs.
     * <p>
     * <p> If there are multiple headers of the same names, the name
     * will map to a comma-separated list containing the values.
     *
     * @param headerPart The <code>header-part</code> of the current
     *                   <code>encapsulation</code>.
     * @return A <code>Map</code> containing the parsed HTTP request headers.
     */
    PartStreamHeaders getParsedHeaders(String headerPart) {
        final int len = headerPart.length();
        PartStreamHeaders headers = new PartStreamHeaders();
        int start = 0;
        for (; ; ) {
            int end = parseEndOfLine(headerPart, start);
            if (start == end) {
                break;
            }
            StringBuilder header = new StringBuilder(headerPart.substring(start, end));
            start = end + 2;
            while (start < len) {
                int nonWs = start;
                while (nonWs < len) {
                    char c = headerPart.charAt(nonWs);
                    if (c != ' ' && c != '\t') {
                        break;
                    }
                    ++nonWs;
                }
                if (nonWs == start) {
                    break;
                }
                // Continuation line found
                end = parseEndOfLine(headerPart, nonWs);
                header.append(" ").append(headerPart.substring(nonWs, end));
                start = end + 2;
            }
            parseHeaderLine(headers, header.toString());
        }
        return headers;
    }

    /**
     * Skips bytes until the end of the current line.
     *
     * @param headerPart The headers, which are being parsed.
     * @param end        Index of the last byte, which has yet been
     *                   processed.
     * @return Index of the \r\n sequence, which indicates
     * end of line.
     */
    private int parseEndOfLine(String headerPart, int end) {
        int index = end;
        for (; ; ) {
            int offset = headerPart.indexOf('\r', index);
            if (offset == -1 || offset + 1 >= headerPart.length()) {
                throw new IllegalStateException(
                        "Expected headers to be terminated by an empty line.");
            }
            if (headerPart.charAt(offset + 1) == '\n') {
                return offset;
            }
            index = offset + 1;
        }
    }

    /**
     * Reads the next header line.
     *
     * @param headers String with all headers.
     * @param header  Map where to store the current header.
     */
    private void parseHeaderLine(PartStreamHeaders headers, String header) {
        final int colonOffset = header.indexOf(':');
        if (colonOffset == -1) {
            // This header line is malformed, skip it.
            return;
        }
        String headerName = header.substring(0, colonOffset).trim();
        String headerValue = header.substring(header.indexOf(':') + 1).trim();
        headers.addHeader(headerName, headerValue);
    }

    /**
     * Called for finding the next item, if any.
     *
     * @return True, if an next item was found, otherwise false.
     * @throws IOException An I/O error occurred.
     */
    private boolean findNextItem() throws IOException {
        if (eof) {
            return false;
        }
        if (currentItem != null) {
            currentItem.close();
            currentItem = null;
        }
        for (;;) {
            boolean nextPart = multi.skipPreamble();
            if (!nextPart) {
                if (currentFieldName == null) {
                    // Outer multipart terminated -> No more data
                    eof = true;
                    return false;
                }
                // Inner multipart terminated -> Return to parsing the outer
                multi.setBoundary(boundary);
                currentFieldName = null;
                continue;
            }
            PartStreamHeaders headers = getParsedHeaders(multi.readHeaders());
            if (currentFieldName == null) {
                // We're parsing the outer multipart
                String fieldName = getFieldName(headers);
                if (fieldName != null) {
                    String subContentType = headers.getHeader(CONTENT_TYPE);
                    if (subContentType != null
                            && subContentType.toLowerCase(Locale.ENGLISH)
                            .startsWith(MULTIPART_MIXED)) {
                        currentFieldName = fieldName;
                        // Multiple files associated with this field name
                        byte[] subBoundary = getBoundary(subContentType);
                        multi.setBoundary(subBoundary);
                        continue;
                    }
                    String fileName = getFileName(headers);
                    currentItem = new PartStreamImpl(fileName,
                            fieldName, headers.getHeader(CONTENT_TYPE),
                            fileName != null, multi.newItemChannel(), headers);
                    itemValid = true;
                    return true;
                }
            } else {
                String fileName = getFileName(headers);
                if (fileName != null) {
                    currentItem = new PartStreamImpl(fileName, currentFieldName,headers.getHeader(CONTENT_TYPE),
                            true, multi.newItemChannel(), headers);
                    itemValid = true;
                    return true;
                }
            }
            multi.discardBodyData();
        }
    }

    /**
     * Returns, whether another instance of {@link PartStream}
     * is available.
     *
     * @return True, if one or more additional file items
     * are available, otherwise false.
     */
    public boolean hasNext() {
        if (eof) {
            return false;
        }
        if (itemValid) {
            return true;
        }
        try {
            return findNextItem();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the next available {@link PartStream}.
     *
     * @return FileItemStream instance, which provides
     * access to the next file item.
     * @throws java.util.NoSuchElementException No more items are
     *                                          available. Use {@link #hasNext()} to prevent this exception.
     */
    public PartStreamImpl next() {
        if (eof || (!itemValid && !hasNext())) {
            throw new NoSuchElementException();
        }
        itemValid = false;
        return currentItem;
    }
}
