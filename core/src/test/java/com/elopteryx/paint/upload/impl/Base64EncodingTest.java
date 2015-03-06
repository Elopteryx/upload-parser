package com.elopteryx.paint.upload.impl;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

public class Base64EncodingTest {

    @Test
    public void these_values_should_work() throws IOException {
        checkEncoding("", "");
        checkEncoding("f", "Zg==");
        checkEncoding("fo", "Zm8=");
        checkEncoding("foo", "Zm9v");
        checkEncoding("foob", "Zm9vYg==");
        checkEncoding("fooba", "Zm9vYmE=");
        checkEncoding("foobar", "Zm9vYmFy");
    }

    @Test(expected = IllegalArgumentException.class)
    public void must_throw_exception_on_invalid_data() throws IOException {
        checkEncoding("f", "Zg=�=");
        checkEncoding("f", "Zg=\u0100=");
    }

    private static void checkEncoding(final String original, String encoded) throws IOException {

        //Directly calling the Jdk decoder
        Base64.Decoder decoder = Base64.getMimeDecoder();
        ByteBuffer output = ByteBuffer.allocate(encoded.length());
        decoder.decode(encoded.getBytes(US_ASCII), output.array());
        byte[] actual = output.array();
        final String jdkResult = new String(actual, US_ASCII).trim();

        //Using the decoder in the parser, which delegates to the Jdk decoder
        MultipartParser.Base64Encoding encoding = new MultipartParser.Base64Encoding();
        encoding.handle(new MultipartParser.PartHandler() {
            @Override
            public void beginPart(PartStreamHeaders headers) {}
            @Override
            public void data(ByteBuffer buffer) throws IOException {
                String parserResult = new String(buffer.array(), US_ASCII).trim();
                assertEquals(jdkResult, original);
                assertEquals(parserResult, original);
            }
            @Override
            public void endPart() throws IOException {}
        }, ByteBuffer.wrap(encoded.getBytes(US_ASCII)));
    }
}
