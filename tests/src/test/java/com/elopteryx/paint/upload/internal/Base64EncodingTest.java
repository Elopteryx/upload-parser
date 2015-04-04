package com.elopteryx.paint.upload.internal;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

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

    @Test(expected = IOException.class)
    public void must_throw_exception_on_invalid_data() throws IOException {
        checkEncoding("f", "Zg=�=");
    }

    private static void checkEncoding(final String original, String encoded) throws IOException {

        MultipartParser.Base64Encoding encoding = new MultipartParser.Base64Encoding();
        encoding.handle(new MultipartParser.PartHandler() {

            @Override
            public void beginPart(Headers headers) {}

            @Override
            public void data(ByteBuffer buffer) throws IOException {
                String parserResult = new String(buffer.array(), US_ASCII).trim();
                assertEquals(parserResult, original);
            }

            @Override
            public void endPart() throws IOException {}

        }, ByteBuffer.wrap(encoded.getBytes(US_ASCII)));
    }
}
