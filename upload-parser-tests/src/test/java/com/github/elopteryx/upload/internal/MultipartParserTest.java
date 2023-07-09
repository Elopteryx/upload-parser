package com.github.elopteryx.upload.internal;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.undertow.util.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class MultipartParserTest {

    private static int[] bufferSizeProvider() {
        return new int[]{2, 10, 1024, 4096};
    }

    @ParameterizedTest
    @MethodSource("bufferSizeProvider")
    void mime_decoding_with_preamble(final int bufferSize) throws IOException {
        final var data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime1.txt"));
        final var handler = new MockPartHandler();
        final var parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, ISO_8859_1);

        final var buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("Here is some text.", handler.parts.get(0).data.toString());
        assertEquals("Here is some more text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }


    @ParameterizedTest
    @MethodSource("bufferSizeProvider")
    void mime_decoding_with_utf8_headers(final int bufferSize) throws IOException {
        final var data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime-utf8.txt"));
        final var handler = new MockPartHandler();
        final var parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, UTF_8);

        final var buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(1, handler.parts.size());
        assertEquals("Just some chinese characters I copied from the internet, no idea what it says.", handler.parts.get(0).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    @ParameterizedTest
    @MethodSource("bufferSizeProvider")
    void mime_decoding_without_preamble(final int bufferSize) throws IOException {
        final var data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime2.txt"));
        final var handler = new MockPartHandler();
        final var parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, ISO_8859_1);

        final var buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("Here is some text.", handler.parts.get(0).data.toString());
        assertEquals("Here is some more text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    @ParameterizedTest
    @MethodSource("bufferSizeProvider")
    void base64_mime_decoding(final int bufferSize) throws IOException {
        final var data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime3.txt"));
        final var handler = new MockPartHandler();
        final var parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, ISO_8859_1);

        final var buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(2, handler.parts.size());
        assertEquals("This is some base64 text.", handler.parts.get(0).data.toString());
        assertEquals("This is some more base64 text.", handler.parts.get(1).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    @ParameterizedTest
    @MethodSource("bufferSizeProvider")
    void quoted_printable(final int bufferSize) throws IOException {
        final var data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime4.txt"));
        final var handler = new MockPartHandler();
        final var parser = MultipartParser.beginParse(handler, "someboundarytext".getBytes(), bufferSize, ISO_8859_1);

        final var buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(1, handler.parts.size());
        assertEquals("time=money.", handler.parts.get(0).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    @ParameterizedTest
    @MethodSource("bufferSizeProvider")
    void mime_decoding_malformed(final int bufferSize) throws IOException {
        final var data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime5_malformed.txt"));
        final var handler = new MockPartHandler();
        final var parser = MultipartParser.beginParse(handler, "someboundarytext".getBytes(), bufferSize, ISO_8859_1);

        final var buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertFalse(parser.isComplete());
    }

    @ParameterizedTest
    @MethodSource("bufferSizeProvider")
    void base64_mime_decoding_malformed(final int bufferSize) {
        final var data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime6_malformed.txt"));
        final var handler = new MockPartHandler();
        final var parser = MultipartParser.beginParse(handler, "unique-boundary-1".getBytes(), bufferSize, ISO_8859_1);

        final var buf = ByteBuffer.wrap(data.getBytes());
        assertThrows(IOException.class, () -> parser.parse(buf));
    }

    @ParameterizedTest
    @MethodSource("bufferSizeProvider")
    void quoted_printable_malformed(final int bufferSize) throws IOException {
        final var data = fixLineEndings(FileUtils.readFile(MultipartParserTest.class, "mime7_malformed.txt"));
        final var handler = new MockPartHandler();
        final var parser = MultipartParser.beginParse(handler, "someboundarytext".getBytes(), bufferSize, ISO_8859_1);

        final var buf = ByteBuffer.wrap(data.getBytes());
        parser.parse(buf);
        assertTrue(parser.isComplete());
        assertEquals(1, handler.parts.size());
        assertEquals("time\nmoney.", handler.parts.get(0).data.toString());

        assertEquals("text/plain", handler.parts.get(0).map.getHeader(Headers.CONTENT_TYPE));
    }

    private static class MockPartHandler implements MultipartParser.PartHandler {

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
        final var builder = new StringBuilder();
        for (var i = 0; i < string.length(); ++i) {
            final var character = string.charAt(i);
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
