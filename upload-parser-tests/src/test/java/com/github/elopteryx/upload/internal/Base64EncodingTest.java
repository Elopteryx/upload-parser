package com.github.elopteryx.upload.internal;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

class Base64EncodingTest {

    @Test
    void these_values_should_work() throws IOException {
        checkEncoding("", "");
        checkEncoding("f", "Zg==");
        checkEncoding("fo", "Zm8=");
        checkEncoding("foo", "Zm9v");
        checkEncoding("foob", "Zm9vYg==");
        checkEncoding("fooba", "Zm9vYmE=");
        checkEncoding("foobar", "Zm9vYmFy");
    }

    @Test
    void must_throw_exception_on_invalid_data() {
        assertThrows(IOException.class, () -> checkEncoding("f", "Zg=�="));
    }

    private static void checkEncoding(final String original, String encoded) throws IOException {

        MultipartParser.Base64Encoding encoding = new MultipartParser.Base64Encoding(1024);
        encoding.handle(new MultipartParser.PartHandler() {

            @Override
            public void beginPart(Headers headers) {}

            @Override
            public void data(ByteBuffer buffer) {
                String parserResult = new String(buffer.array(), US_ASCII).trim();
                assertEquals(parserResult, original);
            }

            @Override
            public void endPart() {}

        }, ByteBuffer.wrap(encoded.getBytes(US_ASCII)));
    }
}
