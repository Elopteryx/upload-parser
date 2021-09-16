/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.elopteryx.upload.internal;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Copied from Undertow.
 *
 * @author Jason T. Greene
 */
class Base64DecoderTest {

    private static final String TOWEL = """
            A towel, it says, is about the most massively useful thing an interstellar hitchhiker can have. Partly it has great practical value - you can wrap it around you for warmth as you bound across the cold moons of Jaglan Beta; you can lie on it on the brilliant marble-sanded beaches of Santraginus V, inhaling the heady sea vapours; you can sleep under it beneath the stars which shine so redly on the desert world of Kakrafoon; use it to sail a mini raft down the slow heavy river Moth; wet it for use in hand-to- hand-combat; wrap it round your head to ward off noxious fumes or to avoid the gaze of the Ravenous Bugblatter Beast of Traal (a mindboggingly stupid animal, it assumes that if you can't see it, it can't see you - daft as a bush, but very ravenous); you can wave your towel in emergencies as a distress signal, and of course dry yourself off with it if it still seems to be clean enough.

            More importantly, a towel has immense psychological value. For some reason, if a strag (strag: non-hitch hiker) discovers that a hitch hiker has his towel with him, he will automatically assume that he is also in possession of a toothbrush, face flannel, soap, tin of biscuits, flask, compass, map, ball of string, gnat spray, wet weather gear, space suit etc., etc. Furthermore, the strag will then happily lend the hitch hiker any of these or a dozen other items that the hitch hiker might accidentally have "lost". What the strag will think is that any man who can hitch the length and breadth of the galaxy, rough it, slum it, struggle against terrible odds, win through, and still knows where his towel is is clearly a man to be reckoned with.
            """;

    private static final String TOWEL_BASE64 = """
            QSB0b3dlbCwgaXQgc2F5cywgaXMgYWJvdXQgdGhlIG1vc3QgbWFzc2l2ZWx5IHVzZWZ1bCB0aGlu\r
            ZyBhbiBpbnRlcnN0ZWxsYXIgaGl0Y2hoaWtlciBjYW4gaGF2ZS4gUGFydGx5IGl0IGhhcyBncmVh\r
            dCBwcmFjdGljYWwgdmFsdWUgLSB5b3UgY2FuIHdyYXAgaXQgYXJvdW5kIHlvdSBmb3Igd2FybXRo\r
            IGFzIHlvdSBib3VuZCBhY3Jvc3MgdGhlIGNvbGQgbW9vbnMgb2YgSmFnbGFuIEJldGE7IHlvdSBj\r
            YW4gbGllIG9uIGl0IG9uIHRoZSBicmlsbGlhbnQgbWFyYmxlLXNhbmRlZCBiZWFjaGVzIG9mIFNh\r
            bnRyYWdpbnVzIFYsIGluaGFsaW5nIHRoZSBoZWFkeSBzZWEgdmFwb3VyczsgeW91IGNhbiBzbGVl\r
            cCB1bmRlciBpdCBiZW5lYXRoIHRoZSBzdGFycyB3aGljaCBzaGluZSBzbyByZWRseSBvbiB0aGUg\r
            ZGVzZXJ0IHdvcmxkIG9mIEtha3JhZm9vbjsgdXNlIGl0IHRvIHNhaWwgYSBtaW5pIHJhZnQgZG93\r
            biB0aGUgc2xvdyBoZWF2eSByaXZlciBNb3RoOyB3ZXQgaXQgZm9yIHVzZSBpbiBoYW5kLXRvLSBo\r
            YW5kLWNvbWJhdDsgd3JhcCBpdCByb3VuZCB5b3VyIGhlYWQgdG8gd2FyZCBvZmYgbm94aW91cyBm\r
            dW1lcyBvciB0byBhdm9pZCB0aGUgZ2F6ZSBvZiB0aGUgUmF2ZW5vdXMgQnVnYmxhdHRlciBCZWFz\r
            dCBvZiBUcmFhbCAoYSBtaW5kYm9nZ2luZ2x5IHN0dXBpZCBhbmltYWwsIGl0IGFzc3VtZXMgdGhh\r
            dCBpZiB5b3UgY2FuJ3Qgc2VlIGl0LCBpdCBjYW4ndCBzZWUgeW91IC0gZGFmdCBhcyBhIGJ1c2gs\r
            IGJ1dCB2ZXJ5IHJhdmVub3VzKTsgeW91IGNhbiB3YXZlIHlvdXIgdG93ZWwgaW4gZW1lcmdlbmNp\r
            ZXMgYXMgYSBkaXN0cmVzcyBzaWduYWwsIGFuZCBvZiBjb3Vyc2UgZHJ5IHlvdXJzZWxmIG9mZiB3\r
            aXRoIGl0IGlmIGl0IHN0aWxsIHNlZW1zIHRvIGJlIGNsZWFuIGVub3VnaC4KCk1vcmUgaW1wb3J0\r
            YW50bHksIGEgdG93ZWwgaGFzIGltbWVuc2UgcHN5Y2hvbG9naWNhbCB2YWx1ZS4gRm9yIHNvbWUg\r
            cmVhc29uLCBpZiBhIHN0cmFnIChzdHJhZzogbm9uLWhpdGNoIGhpa2VyKSBkaXNjb3ZlcnMgdGhh\r
            dCBhIGhpdGNoIGhpa2VyIGhhcyBoaXMgdG93ZWwgd2l0aCBoaW0sIGhlIHdpbGwgYXV0b21hdGlj\r
            YWxseSBhc3N1bWUgdGhhdCBoZSBpcyBhbHNvIGluIHBvc3Nlc3Npb24gb2YgYSB0b290aGJydXNo\r
            LCBmYWNlIGZsYW5uZWwsIHNvYXAsIHRpbiBvZiBiaXNjdWl0cywgZmxhc2ssIGNvbXBhc3MsIG1h\r
            cCwgYmFsbCBvZiBzdHJpbmcsIGduYXQgc3ByYXksIHdldCB3ZWF0aGVyIGdlYXIsIHNwYWNlIHN1\r
            aXQgZXRjLiwgZXRjLiBGdXJ0aGVybW9yZSwgdGhlIHN0cmFnIHdpbGwgdGhlbiBoYXBwaWx5IGxl\r
            bmQgdGhlIGhpdGNoIGhpa2VyIGFueSBvZiB0aGVzZSBvciBhIGRvemVuIG90aGVyIGl0ZW1zIHRo\r
            YXQgdGhlIGhpdGNoIGhpa2VyIG1pZ2h0IGFjY2lkZW50YWxseSBoYXZlICJsb3N0Ii4gV2hhdCB0\r
            aGUgc3RyYWcgd2lsbCB0aGluayBpcyB0aGF0IGFueSBtYW4gd2hvIGNhbiBoaXRjaCB0aGUgbGVu\r
            Z3RoIGFuZCBicmVhZHRoIG9mIHRoZSBnYWxheHksIHJvdWdoIGl0LCBzbHVtIGl0LCBzdHJ1Z2ds\r
            ZSBhZ2FpbnN0IHRlcnJpYmxlIG9kZHMsIHdpbiB0aHJvdWdoLCBhbmQgc3RpbGwga25vd3Mgd2hl\r
            cmUgaGlzIHRvd2VsIGlzIGlzIGNsZWFybHkgYSBtYW4gdG8gYmUgcmVja29uZWQgd2l0aC4K\r
            """;

    private static final String KNOWLEDGE = "Man is distinguished, not only by his reason, but by this singular passion from "
            + "other animals, which is a lust of the mind, that by a perseverance of delight "
            + "in the continued and indefatigable generation of knowledge, exceeds the short "
            + "vehemence of any carnal pleasure.";

    private static final String KNOWLEDGE_ENCODED = """
            TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r
            IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r
            dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r
            dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r
            ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=\r
            """;

    private static final String ILLEGAL_PADDING = """
            TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r
            IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r
            dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r
            dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r
            ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4==\r
            """;

    private static final String ILLEGAL_CHARACTER = """
            TTWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r
            IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r
            dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r
            dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r
            ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4==\r
            """;

    private static final String INVALID_CHARACTER = """
            TW!!?FuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r
            IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r
            dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r
            dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r
            ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=\r
            """;

    private static final String SEVERAL_ILLEGAL_PADDINGS = """
            ====TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r
            IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r
            dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcm====FuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r
            dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r
            ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4======\r
            """;

    @Test
    void decode_buffer() throws IOException {
        final var numbers = new byte[32_768];
        for (var i = 0; i < 32_768; i++) {
            numbers[i] = (byte)(i % 255);
        }
        final var target = ByteBuffer.allocate(65_535);

        final var decoded = ByteBuffer.allocate(numbers.length);
        final var decoder = new Base64Decoder();
        target.flip();
        decoder.decode(target, decoded);

        assertEquals(numbers.length, decoded.remaining());
    }

    @Test
    void draining() throws IOException {
        final var source = ByteBuffer.wrap("c3VyZS4=\r\n\r\n!".getBytes(US_ASCII));
        final var target = ByteBuffer.allocateDirect(100);
        new Base64Decoder().decode(source, target);
        assertEquals((byte) '\r' & 0xFF, source.get() & 0xFF);
        assertEquals((byte) '\n' & 0xFF, source.get() & 0xFF);
        assertEquals((byte) '!' & 0xFF, source.get() & 0xFF);
    }

    @Test
    void decode_string() throws Exception {
        final var buffer = ByteBuffer.wrap(TOWEL.getBytes(US_ASCII));
        buffer.clear();

        new Base64Decoder().decode(ByteBuffer.wrap(TOWEL_BASE64.getBytes(US_ASCII)), buffer);
        assertEquals(TOWEL, new String(buffer.array(), 0, buffer.limit(), US_ASCII));
    }

    @Test
    void decode_string_again() throws Exception {
        final var buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();

        new Base64Decoder().decode(ByteBuffer.wrap(KNOWLEDGE_ENCODED.getBytes(US_ASCII)), buffer);
        assertEquals(KNOWLEDGE, new String(buffer.array(), 0, buffer.limit(), US_ASCII));
    }

    @Test
    void decode_string_null_target() {
        assertThrows(IllegalStateException.class, () -> new Base64Decoder().decode(ByteBuffer.wrap(KNOWLEDGE_ENCODED.getBytes(US_ASCII)), null));
    }

    @Test
    void decode_string_illegal_padding() throws Exception {
        final var buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();
        new Base64Decoder().decode(ByteBuffer.wrap(ILLEGAL_PADDING.getBytes(US_ASCII)), buffer);
    }

    @Test
    void decode_string_illegal_character() throws Exception {
        final var buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();
        new Base64Decoder().decode(ByteBuffer.wrap(ILLEGAL_CHARACTER.getBytes(US_ASCII)), buffer);
    }

    @Test
    void decode_string_several_illegal_padding() {
        final var buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();
        assertThrows(IOException.class, () -> new Base64Decoder().decode(ByteBuffer.wrap(SEVERAL_ILLEGAL_PADDINGS.getBytes(US_ASCII)), buffer));
    }

    @Test
    void decode_string_invalid_character() {
        final var buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();
        assertThrows(IOException.class, () -> new Base64Decoder().decode(ByteBuffer.wrap(INVALID_CHARACTER.getBytes(US_ASCII)), buffer));
    }

    private static final class FlexBase64 {

        private static final byte[] ENCODING_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(US_ASCII);

        /**
         * Creates a state driven base64 encoder.
         *
         * <p>The Encoder instance is not thread-safe, and must not be shared between threads without establishing a
         * happens-before relationship.</p>
         *
         * @return an createEncoder instance
         */
        static Encoder createEncoder() {
            return new Encoder(true);
        }

        /**
         * Controls the encoding process.
         */
        static final class Encoder {
            private int state;
            private int last;
            private int count;
            private final boolean wrap;

            private Encoder(final boolean wrap) {
                this.wrap = wrap;
            }

            /**
             * Encodes bytes read from source and writes them in base64 format to target. If the source limit is hit, this
             * method will return and save the current state, such that future calls can resume the encoding process.
             * In addition, if the target does not have the capacity to fit an entire quad of bytes, this method will also
             * return and save state for subsequent calls to this method. Once all bytes have been encoded to the target,
             * {@link #complete(java.nio.ByteBuffer)} should be called to add the necessary padding characters.
             *
             * @param source the byte buffer to read from
             * @param target the byte buffer to write to
             */
            void encode(final ByteBuffer source, final ByteBuffer target) {
                if (target == null) {
                    throw new IllegalStateException();
                }

                var last = this.last;
                var state = this.state;
                final var wrap = this.wrap;
                var count = this.count;
                final var encodingTable = FlexBase64.ENCODING_TABLE;

                var remaining = source.remaining();
                while (remaining > 0) {
                    // Unrolled state machine for performance (resumes and executes all states in one iteration)
                    var require = 4 - state;
                    require = wrap && (count >= 72) ? require + 2 : require;
                    if (target.remaining() < require) {
                        break;
                    }
                    //  ( 6 | 2) (4 | 4) (2 | 6)
                    var sourceByte = source.get() & 0xFF;
                    if (state == 0) {
                        target.put(encodingTable[sourceByte >>> 2]);
                        last = (sourceByte & 0x3) << 4;
                        state++;
                        if (--remaining <= 0) {
                            break;
                        }
                        sourceByte = source.get() & 0xFF;
                    }
                    if (state == 1) {
                        target.put(encodingTable[last | (sourceByte >>> 4)]);
                        last = (sourceByte & 0x0F) << 2;
                        state++;
                        if (--remaining <= 0) {
                            break;
                        }
                        sourceByte = source.get() & 0xFF;
                    }
                    if (state == 2) {
                        target.put(encodingTable[last | (sourceByte >>> 6)]);
                        target.put(encodingTable[sourceByte & 0x3F]);
                        last = state = 0;
                        remaining--;
                    }

                    if (wrap) {
                        count += 4;
                        if (count >= 76) {
                            count = 0;
                            target.putShort((short)0x0D0A);
                        }
                    }
                }
                this.count = count;
                this.last = last;
                this.state = state;
            }

            /**
             * Completes an encoding session by writing out the necessary padding. This is essential to complying
             * with the Base64 format. This method will write at most 4 or 2 bytes, depending on whether or not wrapping
             * is enabled.
             *
             * @param target the byte buffer to write to
             */
            void complete(final ByteBuffer target) {
                if (state > 0) {
                    target.put(ENCODING_TABLE[last]);
                    for (var i = state; i < 3; i++) {
                        target.put((byte)'=');
                    }

                    last = state = 0;
                }
                if (wrap) {
                    target.putShort((short)0x0D0A);
                }

                count = 0;
            }
        }

    }

    @Test
    void testEncoderDecoderBuffer() throws IOException {
        final var nums = new byte[32_768];
        for (var i = 0; i < 32_768; i++) {
            nums[i] = (byte)(i % 255);
        }

        final var source = ByteBuffer.wrap(nums);
        final var target = ByteBuffer.allocate(65_535);

        final var encoder = FlexBase64.createEncoder();
        encoder.encode(source, target);
        encoder.complete(target);

        final var decoded = ByteBuffer.allocate(nums.length);
        final var decoder = new Base64Decoder();
        target.flip();
        decoder.decode(target, decoded);

        decoded.flip();

        assertEquals(nums.length, decoded.remaining());

        for (final var num : nums) {
            assertEquals(num, decoded.get());
        }
    }

    @Test
    void testEncoderDecoderBufferLoops() throws IOException {
        final var nums = new byte[32_768];
        for (var i = 0; i < 32_768; i++) {
            nums[i] = (byte)(i % 255);
        }
        final var source = ByteBuffer.wrap(nums);
        final var target = ByteBuffer.allocate(65_535);

        final var encoder = FlexBase64.createEncoder();
        var limit = target.limit();
        target.limit(100);
        while (source.remaining() > 0) {
            encoder.encode(source, target);
            var add = limit - target.position();
            add = Math.min(add, 100);
            target.limit(target.limit() + add);
        }
        encoder.complete(target);

        final var decoded = ByteBuffer.allocate(nums.length);
        final var decoder = new Base64Decoder();
        target.flip();

        limit = decoded.limit();
        decoded.limit(100);
        while (target.remaining() > 0) {
            decoder.decode(target, decoded);
            var add = limit - decoded.position();
            add = Math.min(add, 100);
            decoded.limit(decoded.position() + add);
        }

        decoded.flip();

        assertEquals(nums.length, decoded.remaining());

        for (final var num : nums) {
            assertEquals(num, decoded.get());
        }
    }

}
