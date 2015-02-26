package com.elopteryx.paint.upload.impl;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

public class QuotedPrintableEncodingTest {

    @Test
    public void empty_decode() throws IOException {
        checkEncoding("", "");
    }

    @Test
    public void plain_decode() throws IOException {
        checkEncoding("A longer sentence without special characters.", "A longer sentence without special characters.");
    }

    @Test
    public void basic_encode_decode() throws IOException {
        checkEncoding("= Hello world =\r\n", "=3D Hello world =3D=0D=0A");
    }

    @Test
    public void unsafe_decode() throws IOException {
        checkEncoding("=\r\n", "=3D=0D=0A");
    }

    @Test
    public void unsafe_decode_lowercase() throws IOException {
        checkEncoding("=\r\n", "=3d=0d=0a");
    }

    private static void checkEncoding(String original, String encoded) throws IOException {
        MultipartParser.QuotedPrintableEncoding encoding = new MultipartParser.QuotedPrintableEncoding();
        encoding.handle(new MultipartParser.PartHandler() {
            @Override
            public void beginPart(PartStreamHeaders headers) {}
            @Override
            public void data(ByteBuffer buffer) throws IOException {
                String parserResult = new String(buffer.array(), US_ASCII).trim();
                assertEquals(parserResult, original.trim());
            }
            @Override
            public void endPart() throws IOException {}
        }, ByteBuffer.wrap(encoded.getBytes(US_ASCII)));
    }

}
