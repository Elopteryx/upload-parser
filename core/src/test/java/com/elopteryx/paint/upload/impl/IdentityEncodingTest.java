package com.elopteryx.paint.upload.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class IdentityEncodingTest {

    @Test
    public void these_values_should_work() throws IOException {
        checkEncoding("");
        checkEncoding("abc");
        checkEncoding("öüóúőűáéí");
    }

    private static void checkEncoding(final String original) throws IOException {

        MultipartParser.IdentityEncoding encoding = new MultipartParser.IdentityEncoding();
        encoding.handle(new MultipartParser.PartHandler() {

            @Override
            public void beginPart(PartStreamHeaders headers) {}

            @Override
            public void data(ByteBuffer buffer) throws IOException {
                String parserResult = new String(buffer.array(), UTF_8);
                assertEquals(parserResult, original);
            }

            @Override
            public void endPart() throws IOException {}

        }, ByteBuffer.wrap(original.getBytes(UTF_8)));
    }
}
