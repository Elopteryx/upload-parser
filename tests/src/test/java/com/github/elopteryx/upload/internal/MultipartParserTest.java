package com.github.elopteryx.upload.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.undertow.util.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(Parameterized.class)
public class MultipartParserTest {

    private final int bufferSize;

    /**
     * Collects the parameters for each test run.
     * @return The collection of parameter maps
     */
    @Parameterized.Parameters
    public static Collection<Object[]> configs() {
        Map<Integer, Integer> map1 = Collections.singletonMap(1, 2);
        Map<Integer, Integer> map2 = Collections.singletonMap(1, 10);
        Map<Integer, Integer> map3 = Collections.singletonMap(1, 1024);
        Map<Integer, Integer> map4 = Collections.singletonMap(1, 4096);

        return Arrays.asList(new Object[][]{{map1}, {map2}, {map3}, {map4}});
    }

    public MultipartParserTest(Map<Integer, Integer> map) {
        this.bufferSize = map.get(1);
    }

    @Test
    public void creation_works() {
        // Only to achieve 100% method call
        new MultipartParser();
    }

    @Test
    public void mime_decoding_with_preamble() throws IOException {
        String data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime1.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("Here is some text.", handler.parts.get(0).data.toString());
        assertEquals("Here is some more text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }


    @Test
    public void mime_decoding_with_utf8_headers() throws IOException {
        String data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime-utf8.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, UTF_8);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(1, handler.parts.size());
        assertEquals("Just some chinese characters I copied from the internet, no idea what it says.", handler.parts.get(0).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    @Test
    public void mime_decoding_without_preamble() throws IOException {
        String data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime2.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("Here is some text.", handler.parts.get(0).data.toString());
        assertEquals("Here is some more text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    @Test
    public void base64_mime_decoding() throws IOException {
        String data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime3.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("This is some base64 text.", handler.parts.get(0).data.toString());
        assertEquals("This is some more base64 text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    @Test
    public void quoted_printable() throws IOException {
        String data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime4.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "someboundarytext".getBytes(), bufferSize, ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(1, handler.parts.size());
        assertEquals("time=money.", handler.parts.get(0).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    @Test
    public void mime_decoding_malformed() throws IOException {
        String data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime5_malformed.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "someboundarytext".getBytes(), bufferSize, ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertFalse(parser.isComplete());
    }

    @Test(expected = IOException.class)
    public void base64_mime_decoding_malformed() throws IOException {
        String data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime6_malformed.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
    }

    @Test
    public void quoted_printable_malformed() throws IOException {
        String data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime7_malformed.txt"));
        TestPartHandler handler = new TestPartHandler();
        MultipartParser.ParseState parser = MultipartParser.beginParse(handler, "someboundarytext".getBytes(), bufferSize, ISO_8859_1);

        ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(1, handler.parts.size());
        assertEquals("time\nmoney.", handler.parts.get(0).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    private static class TestPartHandler implements MultipartParser.PartHandler {

        private final List<Part> parts = new ArrayList<>();
        private Part current;

        @Override
        public void beginPart(final Headers headers) {
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
        private final Headers map;
        private final StringBuilder data = new StringBuilder();

        private Part(final Headers map) {
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
