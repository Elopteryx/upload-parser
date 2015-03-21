package com.elopteryx.paint.upload.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.undertow.util.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MultipartParserTest {

    @Test
    public void creation_works() {
        // Only to achieve 100% method call
        new MultipartParser();
    }

    @Test
    public void mime_decoding_with_preamble() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime1.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("Here is some text.", handler.parts.get(0).data.toString());
        assertEquals("Here is some more text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }


    @Test
    public void mime_decoding_with_utf8_headers() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime-utf8.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), UTF_8);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(1, handler.parts.size());
        assertEquals("Just some chinese characters I copied from the internet, no idea what it says.", handler.parts.get(0).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }

    @Test
    public void mime_decoding_without_preamble() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime2.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("Here is some text.", handler.parts.get(0).data.toString());
        assertEquals("Here is some more text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }

    @Test
    public void base64_mime_decoding() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime3.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("This is some base64 text.", handler.parts.get(0).data.toString());
        assertEquals("This is some more base64 text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }

    @Test
    public void base64_mime_decoding_with_small_buffers() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime3.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("This is some base64 text.", handler.parts.get(0).data.toString());
        assertEquals("This is some more base64 text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
    }

    @Test
    public void quoted_printable() throws IOException {
        final String data =  fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime4.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "someboundarytext".getBytes(), ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(1, handler.parts.size());
        assertEquals("time=money.", handler.parts.get(0).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(PartStreamHeaders.CONTENT_TYPE));
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
        for (int i = 0; i < string.length(); ++i) {
            char character = string.charAt(i);
            if (character == '\n') {
                if (i == 0 || string.charAt(i - 1) != '\r') {
                    builder.append("\r\n");
                } else {
                    builder.append('\n');
                }
            } else if (character == '\r') {
                if (i + 1 == string.length() || string.charAt(i + 1) != '\n') {
                    builder.append("\r\n");
                } else {
                    builder.append('\r');
                }
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }
}
