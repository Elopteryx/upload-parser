/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.elopteryx.paint.upload.impl;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

public class Base64EncodingTest {

    /**
     * Tests RFC 4648 section 10 test vectors.
     * <ul>
     * <li>BASE64("") = ""</li>
     * <li>BASE64("f") = "Zg=="</li>
     * <li>BASE64("fo") = "Zm8="</li>
     * <li>BASE64("foo") = "Zm9v"</li>
     * <li>BASE64("foob") = "Zm9vYg=="</li>
     * <li>BASE64("fooba") = "Zm9vYmE="</li>
     * <li>BASE64("foobar") = "Zm9vYmFy"</li>
     * </ul>
     *
     * @see <a href="http://tools.ietf.org/html/rfc4648">http://tools.ietf.org/html/rfc4648</a>
     */
    @Test
    public void rfc4648Section10Decode() throws Exception {
        assertEncoding("", "");
        assertEncoding("f", "Zg==");
        assertEncoding("fo", "Zm8=");
        assertEncoding("foo", "Zm9v");
        assertEncoding("foob", "Zm9vYg==");
        assertEncoding("fooba", "Zm9vYmE=");
        assertEncoding("foobar", "Zm9vYmFy");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonASCIIcharacter() throws Exception {
        assertEncoding("f", "Zg=À=");
        assertEncoding("f", "Zg=\u0100=");
    }

    private static void assertEncoding(String clearText, String encoded) throws Exception {

        //Directly calling the Jdk decoder
        Base64.Decoder decoder = Base64.getMimeDecoder();
        ByteBuffer output = ByteBuffer.allocate(encoded.length());
        decoder.decode(encoded.getBytes(US_ASCII), output.array());
        byte[] actual = output.array();
        String jdkResult = new String(actual, US_ASCII).trim();

        //Using the decoder in the parser, which delegates to the Jdk decoder
        MultipartParser.Base64Encoding encoding = new MultipartParser.Base64Encoding();
        encoding.handle(new MultipartParser.PartHandler() {
            @Override
            public void beginPart(PartStreamHeaders headers) {}
            @Override
            public void data(ByteBuffer buffer) throws IOException {
                String parserResult = new String(buffer.array(), US_ASCII).trim();
                assertEquals(jdkResult, clearText);
                assertEquals(parserResult, clearText);
            }
            @Override
            public void endPart() throws IOException {}
        }, ByteBuffer.wrap(encoded.getBytes(US_ASCII)));
    }
}
