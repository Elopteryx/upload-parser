package com.github.elopteryx.upload.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

class IdentityEncodingTest {

    @Test
    void these_values_should_work() throws IOException {
        checkEncoding("");
        checkEncoding("abc");
        checkEncoding("öüóúőűáéí");
    }

    private static void checkEncoding(final String original) throws IOException {

        MultipartParser.IdentityEncoding encoding = new MultipartParser.IdentityEncoding();
        encoding.handle(new MultipartParser.PartHandler() {

            @Override
            public void beginPart(Headers headers) {}

            @Override
            public void data(ByteBuffer buffer) {
                String parserResult = new String(buffer.array(), UTF_8);
                assertEquals(parserResult, original);
            }

            @Override
            public void endPart() {}

        }, ByteBuffer.wrap(original.getBytes(UTF_8)));
    }
}
