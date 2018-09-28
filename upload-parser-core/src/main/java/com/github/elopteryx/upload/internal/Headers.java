/*
 * Copyright (C) 2016 Adam Forgacs
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

package com.github.elopteryx.upload.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class is used to extract, store and retrieve header keys
 * and values. Supports the HTTP request headers and also the headers
 * for the part items received in the multipart request.
 */
public class Headers {

    private static final String BOUNDARY = "boundary";

    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    public static final String CONTENT_ENCODING = "Content-Encoding";

    public static final String CONTENT_LENGTH = "Content-Length";

    public static final String CONTENT_TYPE = "Content-Type";

    private final Map<String, List<String>> headerNameToValueListMap = new LinkedHashMap<>();

    String getHeader(final String name) {
        final var nameLower = name.toLowerCase(Locale.ENGLISH);
        final var headerValueList = headerNameToValueListMap.get(nameLower);
        return headerValueList != null ? headerValueList.get(0) : null;
    }

    Collection<String> getHeaders(final String name) {
        final var nameLower = name.toLowerCase(Locale.ENGLISH);
        return headerNameToValueListMap.getOrDefault(nameLower, Collections.emptyList());
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
    void addHeader(final String name, final String value) {
        final var nameLower = name.toLowerCase(Locale.ENGLISH);
        headerNameToValueListMap.computeIfAbsent(nameLower, key -> new ArrayList<>()).add(value);
    }

    /**
     * Extracts a token from a header that has a given key. For instance if the header is
     * content-type=multipart/form-data boundary=myboundary
     * and the key is boundary the myboundary will be returned.
     *
     * @param header The header
     * @return The token, or null if it was not found
     */
    public static String extractBoundaryFromHeader(final String header) {

        final var pos = header.indexOf(BOUNDARY + '=');
        if (pos == -1) {
            return null;
        }
        int end;
        final var start = pos + BOUNDARY.length() + 1;
        for (end = start; end < header.length(); ++end) {
            final var character = header.charAt(end);
            if (character == ' ' || character == '\t' || character == ';') {
                break;
            }
        }
        return header.substring(start, end);
    }

    /**
     * Extracts a quoted value from a header that has a given key. For instance if the header is
     * content-disposition=form-data; name="my field"
     * and the key is name then "my field" will be returned without the quotes.
     *
     * @param header The header
     * @param key    The key that identifies the token to extract
     * @return The token, or null if it was not found
     */
    public static String extractQuotedValueFromHeader(final String header, final String key) {

        var keyPosition = 0;
        var pos = -1;
        var inQuotes = false;
        for (var i = 0; i < header.length() - 1; ++i) { //-1 because we need room for the = at the end
            final var character = header.charAt(i);
            if (inQuotes) {
                if (character == '"') {
                    inQuotes = false;
                }
            } else {
                if (key.charAt(keyPosition) == character) {
                    keyPosition++;
                } else if (character == '"') {
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
        var start = pos;
        if (header.charAt(start) == '"') {
            start++;
            for (end = start; end < header.length(); ++end) {
                final var character = header.charAt(end);
                if (character == '"') {
                    break;
                }
            }
            return header.substring(start, end);

        } else {
            //no quotes
            for (end = start; end < header.length(); ++end) {
                final var character = header.charAt(end);
                if (character == ' ' || character == '\t') {
                    break;
                }
            }
            return header.substring(start, end);
        }
    }

}
