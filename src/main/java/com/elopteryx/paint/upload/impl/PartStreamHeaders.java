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

import java.util.*;

/**
 * This class provides support for accessing the headers for a file or form
 * item that was received within a multipart/form-data POST request.
 */
class PartStreamHeaders {

    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    public static final String CONTENT_TYPE = "Content-Type";

    private final Map<String, List<String>> headerNameToValueListMap = new LinkedHashMap<>();

    String getHeader(String name) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        List<String> headerValueList = headerNameToValueListMap.get(nameLower);
        return headerValueList != null ? headerValueList.get(0) : null;
    }

    Collection<String> getHeaders(String name) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        Collection<String> headers = headerNameToValueListMap.get(nameLower);
        return headers != null ? headers : Collections.<String>emptyList();
    }

    Collection<String> getHeaderNames() {
        return headerNameToValueListMap.keySet();
    }

    /**
     * Method to add header values to this instance.
     *
     * @param name  name of this header
     * @param value value of this header
     */
    void addHeader(String name, String value) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        //headerNameToValueListMap.computeIfAbsent(nameLower, n -> new ArrayList<>()).add(value); Java 8 version
        List<String> headers = headerNameToValueListMap.get(nameLower);
        if(headers == null) {
            headers = new ArrayList<>();
            headerNameToValueListMap.put(nameLower, headers);
        }
        headers.add(value);
    }

    /**
     * Extracts a token from a header that has a given key. For instance if the header is
     * <p/>
     * content-type=multipart/form-data boundary=myboundary
     * and the key is boundary the myboundary will be returned.
     *
     * @param header The header
     * @param key    The key that identifies the token to extract
     * @return The token, or null if it was not found
     */
    static String extractTokenFromHeader(final String header, final String key) {
        int pos = header.indexOf(key + '=');
        if (pos == -1) {
            return null;
        }
        int end;
        int start = pos + key.length() + 1;
        for (end = start; end < header.length(); ++end) {
            char c = header.charAt(end);
            if (c == ' ' || c == '\t') {
                break;
            }
        }
        return header.substring(start, end);
    }

    /**
     * Extracts a quoted value from a header that has a given key. For instance if the header is
     * <p/>
     * content-disposition=form-data; name="my field"
     * and the key is name then "my field" will be returned without the quotes.
     *
     * @param header The header
     * @param key    The key that identifies the token to extract
     * @return The token, or null if it was not found
     */
    static String extractQuotedValueFromHeader(final String header, final String key) {

        int keyPosition = 0;
        int pos = -1;
        boolean inQuotes = false;
        for (int i = 0; i < header.length() - 1; ++i) { //-1 because we need room for the = at the end
            //TODO: a more efficient matching algorithm
            char c = header.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    inQuotes = false;
                }
            } else {
                if (key.charAt(keyPosition) == c) {
                    keyPosition++;
                } else if (c == '"') {
                    keyPosition = 0;
                    inQuotes = true;
                } else {
                    keyPosition = 0;
                }
                if (keyPosition == key.length()) {
                    if (header.charAt(i + 1) == '=') {
                        pos = i + 2;
                        break;
                    } else {
                        keyPosition = 0;
                    }
                }
            }

        }
        if (pos == -1) {
            return null;
        }

        int end;
        int start = pos;
        if (header.charAt(start) == '"') {
            start++;
            for (end = start; end < header.length(); ++end) {
                char c = header.charAt(end);
                if (c == '"') {
                    break;
                }
            }
            return header.substring(start, end);

        } else {
            //no quotes
            for (end = start; end < header.length(); ++end) {
                char c = header.charAt(end);
                if (c == ' ' || c == '\t') {
                    break;
                }
            }
            return header.substring(start, end);
        }
    }

}
