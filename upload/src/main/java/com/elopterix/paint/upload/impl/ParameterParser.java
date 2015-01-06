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

import com.elopterix.paint.upload.util.MimeUtility;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A simple parser intended to parse sequences of name/value pairs.
 * <p>
 * Parameter values are expected to be enclosed in quotes if they
 * contain unsafe characters, such as '=' characters or separators.
 * Parameter values are optional and can be omitted.
 * <p>
 * <p>
 * <code>param1 = value; param2 = "anything goes; really"; param3</code>
 * </p>
 */
class ParameterParser {

    /**
     * String to be parsed.
     */
    private char[] chars = null;

    /**
     * Current position in the string.
     */
    private int pos = 0;

    /**
     * Maximum position in the string.
     */
    private int len = 0;

    /**
     * Start of a token.
     */
    private int i1 = 0;

    /**
     * End of a token.
     */
    private int i2 = 0;

    /**
     * Are there any characters left to parse?
     *
     * @return <tt>true</tt> if there are unparsed characters,
     * <tt>false</tt> otherwise.
     */
    private boolean hasChar() {
        return pos < len;
    }

    /**
     * A helper method to process the parsed token. This method removes
     * leading and trailing blanks as well as enclosing quotation marks,
     * when necessary.
     *
     * @param quoted <tt>true</tt> if quotation marks are expected,
     *               <tt>false</tt> otherwise.
     * @return the token
     */
    private String getToken(boolean quoted) {
        // Trim leading white spaces
        while ((i1 < i2) && (Character.isWhitespace(chars[i1]))) {
            i1++;
        }
        // Trim trailing white spaces
        while ((i2 > i1) && (Character.isWhitespace(chars[i2 - 1]))) {
            i2--;
        }
        // Strip away quotation marks if necessary
        if (quoted
                && ((i2 - i1) >= 2)
                && (chars[i1] == '"')
                && (chars[i2 - 1] == '"')) {
            i1++;
            i2--;
        }
        String result = null;
        if (i2 > i1) {
            result = new String(chars, i1, i2 - i1);
        }
        return result;
    }

    /**
     * Tests if the given character is present in the array of characters.
     *
     * @param ch        the character to test for presense in the array of characters
     * @param charArray the array of characters to test against
     * @return <tt>true</tt> if the character is present in the array of
     * characters, <tt>false</tt> otherwise.
     */
    private boolean isOneOf(char ch, final char[] charArray) {
        for (char element : charArray) {
            if (ch == element) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses out a token until any of the given terminators
     * is encountered.
     *
     * @param terminators the array of terminating characters. Any of these
     *                    characters when encountered signify the end of the token
     * @return the token
     */
    private String parseToken(final char[] terminators) {
        char ch;
        i1 = pos;
        i2 = pos;
        while (hasChar()) {
            ch = chars[pos];
            if (isOneOf(ch, terminators)) {
                break;
            }
            i2++;
            pos++;
        }
        return getToken(false);
    }

    /**
     * Parses out a token until any of the given terminators
     * is encountered outside the quotation marks.
     *
     * @param terminators the array of terminating characters. Any of these
     *                    characters when encountered outside the quotation marks signify the end
     *                    of the token
     * @return the token
     */
    private String parseQuotedToken(final char[] terminators) {
        char ch;
        i1 = pos;
        i2 = pos;
        boolean quoted = false;
        boolean charEscaped = false;
        while (hasChar()) {
            ch = chars[pos];
            if (!quoted && isOneOf(ch, terminators)) {
                break;
            }
            if (!charEscaped && ch == '"') {
                quoted = !quoted;
            }
            charEscaped = (!charEscaped && ch == '\\');
            i2++;
            pos++;

        }
        return getToken(true);
    }

    /**
     * Extracts a map of name/value pairs from the given string. Names are
     * expected to be unique. Multiple separators may be specified and
     * the earliest found in the input string is used.
     *
     * @param str        the string that contains a sequence of name/value pairs
     * @param separators the name/value pairs separators
     * @return a map of name/value pairs
     */
    public Map<String, String> parse(final String str, char... separators) {
        if (str == null || separators == null || separators.length == 0) {
            return new HashMap<>();
        }
        char separator = separators[0];
        int idx = str.length();
        for (char separator2 : separators) {
            int tmp = str.indexOf(separator2);
            if (tmp != -1 && tmp < idx) {
                idx = tmp;
                separator = separator2;
            }
        }
        char[] charArray = str.toCharArray();
        int offset = 0;
        int length = charArray.length;
        Map<String, String> params = new HashMap<>();
        this.chars = charArray;
        this.pos = offset;
        this.len = length;

        String paramName;
        String paramValue;
        while (hasChar()) {
            paramName = parseToken(new char[]{'=', separator});
            paramValue = null;
            if (hasChar() && (charArray[pos] == '=')) {
                pos++; // skip '='
                paramValue = parseQuotedToken(new char[]{separator});

                if (paramValue != null) {
                    try {
                        paramValue = MimeUtility.decodeText(paramValue);
                    } catch (UnsupportedEncodingException e) {
                        // let's keep the original value in this case
                    }
                }
            }
            if (hasChar() && (charArray[pos] == separator)) {
                pos++; // skip separator
            }
            if (paramName != null && !paramName.isEmpty()) {
                paramName = paramName.toLowerCase(Locale.ENGLISH);
                params.put(paramName, paramValue);
            }
        }
        return params;
    }

}
