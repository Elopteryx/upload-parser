package com.elopteryx.paint.upload.impl;

import io.undertow.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MultipartParserTest {

    @Test
    public void testSimpleMimeDecodingWithPreamble() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime1.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), StandardCharsets.ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        Assert.assertTrue(parser.isComplete());
        Assert.assertEquals(2, handler.parts.size());
        Assert.assertEquals("Here is some text.", handler.parts.get(0).data.toString());
        Assert.assertEquals("Here is some more text.", handler.parts.get(1).data.toString());

        Assert.assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }


    @Test
    public void testMimeDecodingWithUTF8Headers() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime-utf8.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        Assert.assertTrue(parser.isComplete());
        Assert.assertEquals(1, handler.parts.size());
        Assert.assertEquals("Just some chinese characters I copied from the internet, no idea what it says.", handler.parts.get(0).data.toString());

        Assert.assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
        Assert.assertEquals("attachment; filename=个专为语文教学而设计的电脑软件.txt", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_DISPOSITION));
    }

    @Test
    public void testSimpleMimeDecodingWithoutPreamble() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime2.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), StandardCharsets.ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        Assert.assertTrue(parser.isComplete());
        Assert.assertEquals(2, handler.parts.size());
        Assert.assertEquals("Here is some text.", handler.parts.get(0).data.toString());
        Assert.assertEquals("Here is some more text.", handler.parts.get(1).data.toString());

        Assert.assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }

    @Test
    public void testBase64MimeDecoding() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime3.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), StandardCharsets.ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        Assert.assertTrue(parser.isComplete());
        Assert.assertEquals(2, handler.parts.size());
        Assert.assertEquals("This is some base64 text.", handler.parts.get(0).data.toString());
        Assert.assertEquals("This is some more base64 text.", handler.parts.get(1).data.toString());

        Assert.assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }

    @Test
    public void testBase64MimeDecodingWithSmallBuffers() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime3.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), StandardCharsets.ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        Assert.assertTrue(parser.isComplete());
        Assert.assertEquals(2, handler.parts.size());
        Assert.assertEquals("This is some base64 text.", handler.parts.get(0).data.toString());
        Assert.assertEquals("This is some more base64 text.", handler.parts.get(1).data.toString());

        Assert.assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }

    @Test
    public void testQuotedPrintable() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime4.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "someboundarytext".getBytes(), StandardCharsets.ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        Assert.assertTrue(parser.isComplete());
        Assert.assertEquals(1, handler.parts.size());
        Assert.assertEquals("time=money.", handler.parts.get(0).data.toString());

        Assert.assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }

    private static class TestPartHandler implements MultipartParser.PartHandler {

        private final List<Part> parts = new ArrayList<>();
        private Part current;


        @Override
        public void beginPart(final PartStreamHeaders headers) {
            current = new Part(headers);
            parts.add(current);
        }

        @Override
        public void data(final ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                current.data.append((char) buffer.get());
            }
        }

        @Override
        public void endPart() {

        }
    }

    private static class Part {
        private final PartStreamHeaders map;
        private final StringBuilder data = new StringBuilder();

        private Part(final PartStreamHeaders map) {
            this.map = map;
        }
    }

    private static String fixLineEndings(final String string) {
        final StringBuilder builder = new StringBuilder();
        for(int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if(c == '\n') {
                if(i == 0 || string.charAt(i-1) != '\r') {
                    builder.append("\r\n");
                } else {
                    builder.append('\n');
                }
            } else if(c == '\r') {
                if(i+1 == string.length() || string.charAt(i+1) != '\n') {
                    builder.append("\r\n");
                } else {
                    builder.append('\r');
                }
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
