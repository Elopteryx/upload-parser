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
package com.elopterix.paint.upload.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class QuotedPrintableDecoder {

    /**
     * The shift value required to create the upper nibble
     * from the first of 2 byte values converted from ascii hex.
     */
    private static final int UPPER_NIBBLE_SHIFT = Byte.SIZE / 2;

    /**
     * Hidden constructor, this class must not be instantiated.
     */
    private QuotedPrintableDecoder() {
        // do nothing
    }

    /**
     * Decode the encoded byte data writing it to the given output stream.
     *
     * @param data The array of byte data to decode.
     * @return the number of bytes produced.
     * @throws IOException
     */
    public static byte[] decode(byte[] data) throws IOException {
        int off = 0;
        int length = data.length;
        int endOffset = off + length;

        ByteArrayOutputStream out = new ByteArrayOutputStream(length);
        while (off < endOffset) {
            byte ch = data[off++];

            // space characters were translated to '_' on encode, so we need to translate them back.
            if (ch == '_') {
                out.write(' ');
            } else if (ch == '=') {
                // we found an encoded character.  Reduce the 3 char sequence to one.
                // but first, make sure we have two characters to work with.
                if (off + 1 >= endOffset) {
                    throw new IOException("Invalid quoted printable encoding; truncated escape sequence");
                }

                byte b1 = data[off++];
                byte b2 = data[off++];

                // we've found an encoded carriage return.  The next char needs to be a newline
                if (b1 == '\r') {
                    if (b2 != '\n') {
                        throw new IOException("Invalid quoted printable encoding; CR must be followed by LF");
                    }
                    // this was a soft linebreak inserted by the encoding.  We just toss this away
                    // on decode.
                } else {
                    // this is a hex pair we need to convert back to a single byte.
                    int c1 = Character.digit((char) b1, 16);
                    if (c1 == -1) {
                        throw new IOException("Invalid quoted printable encoding: not a valid hex digit: " + b1);
                    }
                    int c2 = Character.digit((char) b2, 16);
                    if (c2 == -1) {
                        throw new IOException("Invalid quoted printable encoding: not a valid hex digit: " + b2);
                    }
                    out.write((c1 << UPPER_NIBBLE_SHIFT) | c2);
                    // 3 bytes in, one byte out
                }
            } else {
                // simple character, just write it out.
                out.write(ch);
            }
        }
        return out.toByteArray();
    }
}
