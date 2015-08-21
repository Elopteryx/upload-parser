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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Jason T. Greene
 */
public class Base64DecoderTest {

    private static final String TOWEL = "A towel, it says, is about the most massively useful thing an interstellar "
            + "hitchhiker can have. Partly it has great practical value - you can wrap it around you for warmth as you "
            + "bound across the cold moons of Jaglan Beta; you can lie on it on the brilliant marble-sanded beaches of "
            +  "Santraginus V, inhaling the heady sea vapours; you can sleep under it beneath the stars which shine so "
            + "redly on the desert world of Kakrafoon; use it to sail a mini raft down the slow heavy river Moth; wet "
            + "it for use in hand-to- hand-combat; wrap it round your head to ward off noxious fumes or to avoid the "
            + "gaze of the Ravenous Bugblatter Beast of Traal (a mindboggingly stupid animal, it assumes that if you "
            + "can't see it, it can't see you - daft as a bush, but very ravenous); you can wave your towel in "
            + "emergencies as a distress signal, and of course dry yourself off with it if it still seems to be clean "
            + "enough."
            + "\n\n"
            + "More importantly, a towel has immense psychological value. For some reason, if a strag "
            + "(strag: non-hitch hiker) discovers that a hitch hiker has his towel with him, he will automatically "
            + "assume that he is also in possession of a toothbrush, face flannel, soap, tin of biscuits, flask, compass, "
            + "map, ball of string, gnat spray, wet weather gear, space suit etc., etc. Furthermore, the strag will then "
            + "happily lend the hitch hiker any of these or a dozen other items that the hitch hiker might accidentally "
            + "have \"lost\". What the strag will think is that any man who can hitch the length and breadth of the "
            + "galaxy, rough it, slum it, struggle against terrible odds, win through, and still knows where his towel "
            + "is is clearly a man to be reckoned with.\n";

    private static final String TOWEL_BASE64 = "QSB0b3dlbCwgaXQgc2F5cywgaXMgYWJvdXQgdGhlIG1vc3QgbWFzc2l2ZWx5IHVzZWZ1bCB0aGlu\r\n"
            + "ZyBhbiBpbnRlcnN0ZWxsYXIgaGl0Y2hoaWtlciBjYW4gaGF2ZS4gUGFydGx5IGl0IGhhcyBncmVh\r\n"
            + "dCBwcmFjdGljYWwgdmFsdWUgLSB5b3UgY2FuIHdyYXAgaXQgYXJvdW5kIHlvdSBmb3Igd2FybXRo\r\n"
            + "IGFzIHlvdSBib3VuZCBhY3Jvc3MgdGhlIGNvbGQgbW9vbnMgb2YgSmFnbGFuIEJldGE7IHlvdSBj\r\n"
            + "YW4gbGllIG9uIGl0IG9uIHRoZSBicmlsbGlhbnQgbWFyYmxlLXNhbmRlZCBiZWFjaGVzIG9mIFNh\r\n"
            + "bnRyYWdpbnVzIFYsIGluaGFsaW5nIHRoZSBoZWFkeSBzZWEgdmFwb3VyczsgeW91IGNhbiBzbGVl\r\n"
            + "cCB1bmRlciBpdCBiZW5lYXRoIHRoZSBzdGFycyB3aGljaCBzaGluZSBzbyByZWRseSBvbiB0aGUg\r\n"
            + "ZGVzZXJ0IHdvcmxkIG9mIEtha3JhZm9vbjsgdXNlIGl0IHRvIHNhaWwgYSBtaW5pIHJhZnQgZG93\r\n"
            + "biB0aGUgc2xvdyBoZWF2eSByaXZlciBNb3RoOyB3ZXQgaXQgZm9yIHVzZSBpbiBoYW5kLXRvLSBo\r\n"
            + "YW5kLWNvbWJhdDsgd3JhcCBpdCByb3VuZCB5b3VyIGhlYWQgdG8gd2FyZCBvZmYgbm94aW91cyBm\r\n"
            + "dW1lcyBvciB0byBhdm9pZCB0aGUgZ2F6ZSBvZiB0aGUgUmF2ZW5vdXMgQnVnYmxhdHRlciBCZWFz\r\n"
            + "dCBvZiBUcmFhbCAoYSBtaW5kYm9nZ2luZ2x5IHN0dXBpZCBhbmltYWwsIGl0IGFzc3VtZXMgdGhh\r\n"
            + "dCBpZiB5b3UgY2FuJ3Qgc2VlIGl0LCBpdCBjYW4ndCBzZWUgeW91IC0gZGFmdCBhcyBhIGJ1c2gs\r\n"
            + "IGJ1dCB2ZXJ5IHJhdmVub3VzKTsgeW91IGNhbiB3YXZlIHlvdXIgdG93ZWwgaW4gZW1lcmdlbmNp\r\n"
            +  "ZXMgYXMgYSBkaXN0cmVzcyBzaWduYWwsIGFuZCBvZiBjb3Vyc2UgZHJ5IHlvdXJzZWxmIG9mZiB3\r\n"
            +  "aXRoIGl0IGlmIGl0IHN0aWxsIHNlZW1zIHRvIGJlIGNsZWFuIGVub3VnaC4KCk1vcmUgaW1wb3J0\r\n"
            + "YW50bHksIGEgdG93ZWwgaGFzIGltbWVuc2UgcHN5Y2hvbG9naWNhbCB2YWx1ZS4gRm9yIHNvbWUg\r\n"
            + "cmVhc29uLCBpZiBhIHN0cmFnIChzdHJhZzogbm9uLWhpdGNoIGhpa2VyKSBkaXNjb3ZlcnMgdGhh\r\n"
            + "dCBhIGhpdGNoIGhpa2VyIGhhcyBoaXMgdG93ZWwgd2l0aCBoaW0sIGhlIHdpbGwgYXV0b21hdGlj\r\n"
            + "YWxseSBhc3N1bWUgdGhhdCBoZSBpcyBhbHNvIGluIHBvc3Nlc3Npb24gb2YgYSB0b290aGJydXNo\r\n"
            + "LCBmYWNlIGZsYW5uZWwsIHNvYXAsIHRpbiBvZiBiaXNjdWl0cywgZmxhc2ssIGNvbXBhc3MsIG1h\r\n"
            + "cCwgYmFsbCBvZiBzdHJpbmcsIGduYXQgc3ByYXksIHdldCB3ZWF0aGVyIGdlYXIsIHNwYWNlIHN1\r\n"
            + "aXQgZXRjLiwgZXRjLiBGdXJ0aGVybW9yZSwgdGhlIHN0cmFnIHdpbGwgdGhlbiBoYXBwaWx5IGxl\r\n"
            + "bmQgdGhlIGhpdGNoIGhpa2VyIGFueSBvZiB0aGVzZSBvciBhIGRvemVuIG90aGVyIGl0ZW1zIHRo\r\n"
            + "YXQgdGhlIGhpdGNoIGhpa2VyIG1pZ2h0IGFjY2lkZW50YWxseSBoYXZlICJsb3N0Ii4gV2hhdCB0\r\n"
            + "aGUgc3RyYWcgd2lsbCB0aGluayBpcyB0aGF0IGFueSBtYW4gd2hvIGNhbiBoaXRjaCB0aGUgbGVu\r\n"
            + "Z3RoIGFuZCBicmVhZHRoIG9mIHRoZSBnYWxheHksIHJvdWdoIGl0LCBzbHVtIGl0LCBzdHJ1Z2ds\r\n"
            + "ZSBhZ2FpbnN0IHRlcnJpYmxlIG9kZHMsIHdpbiB0aHJvdWdoLCBhbmQgc3RpbGwga25vd3Mgd2hl\r\n"
            + "cmUgaGlzIHRvd2VsIGlzIGlzIGNsZWFybHkgYSBtYW4gdG8gYmUgcmVja29uZWQgd2l0aC4K\r\n";

    private static final String KNOWLEDGE = "Man is distinguished, not only by his reason, but by this singular passion from "
            + "other animals, which is a lust of the mind, that by a perseverance of delight "
            + "in the continued and indefatigable generation of knowledge, exceeds the short "
            + "vehemence of any carnal pleasure.";

    private static final String KNOWLEDGE_ENCODED = "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r\n"
            + "IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r\n"
            + "dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r\n"
            + "dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r\n"
            + "ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=\r\n";

    private static final String ILLEGAL_PADDING = "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r\n"
            + "IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r\n"
            + "dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r\n"
            + "dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r\n"
            + "ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4==\r\n";

    private static final String ILLEGAL_CHARACTER = "T"
            + "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r\n"
            + "IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r\n"
            + "dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r\n"
            + "dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r\n"
            + "ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4==\r\n";

    private static final String INVALID_CHARACTER = "TW!!?FuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r\n"
            + "IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r\n"
            + "dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r\n"
            + "dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r\n"
            + "ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=\r\n";

    private static final String SEVERAL_ILLEGAL_PADDINGS = "====TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz\r\n"
            + "IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\r\n"
            + "dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcm====FuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\r\n"
            + "dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\r\n"
            + "ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4======\r\n";

    @Test
    public void decode_buffer() throws IOException {
        byte[] numbers = new byte[32768];
        for (int i = 0; i < 32768; i++) {
            numbers[i] = (byte)(i % 255);
        }
        ByteBuffer target = ByteBuffer.allocate(65535);

        ByteBuffer decoded = ByteBuffer.allocate(numbers.length);
        Base64Decoder decoder = new Base64Decoder();
        target.flip();
        decoder.decode(target, decoded);

        Assert.assertEquals(numbers.length, decoded.remaining());
    }

    @Test
    public void draining() throws IOException {
        byte[] bytes = "c3VyZS4=\r\n\r\n!".getBytes("US-ASCII");
        ByteBuffer source = ByteBuffer.wrap(bytes);
        ByteBuffer target = ByteBuffer.allocateDirect(100);
        new Base64Decoder().decode(source, target);
        Assert.assertEquals((byte) '\r' & 0xFF, source.get() & 0xFF);
        Assert.assertEquals((byte) '\n' & 0xFF, source.get() & 0xFF);
        Assert.assertEquals((byte) '!' & 0xFF, source.get() & 0xFF);
    }

    @Test
    public void decode_string() throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(TOWEL.getBytes(US_ASCII));
        buffer.clear();

        new Base64Decoder().decode(ByteBuffer.wrap(TOWEL_BASE64.getBytes(US_ASCII)), buffer);
        Assert.assertEquals(TOWEL, new String(buffer.array(), 0, buffer.limit(), US_ASCII));
    }

    @Test
    public void decode_string_again() throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();

        new Base64Decoder().decode(ByteBuffer.wrap(KNOWLEDGE_ENCODED.getBytes(US_ASCII)), buffer);
        Assert.assertEquals(KNOWLEDGE, new String(buffer.array(), 0, buffer.limit(), US_ASCII));
    }

    @Test(expected = IllegalStateException.class)
    public void decode_string_null_target() throws Exception {
        new Base64Decoder().decode(ByteBuffer.wrap(KNOWLEDGE_ENCODED.getBytes(US_ASCII)), null);
    }

    @Test
    public void decode_string_illegal_padding() throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();
        new Base64Decoder().decode(ByteBuffer.wrap(ILLEGAL_PADDING.getBytes(US_ASCII)), buffer);
    }

    @Test
    public void decode_string_illegal_character() throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();
        new Base64Decoder().decode(ByteBuffer.wrap(ILLEGAL_CHARACTER.getBytes(US_ASCII)), buffer);
    }

    @Test(expected = IOException.class)
    public void decode_string_several_illegal_padding() throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();
        new Base64Decoder().decode(ByteBuffer.wrap(SEVERAL_ILLEGAL_PADDINGS.getBytes(US_ASCII)), buffer);
    }

    @Test(expected = IOException.class)
    public void decode_string_invalid_character() throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(KNOWLEDGE.getBytes(US_ASCII));
        buffer.clear();
        new Base64Decoder().decode(ByteBuffer.wrap(INVALID_CHARACTER.getBytes(US_ASCII)), buffer);
    }

    private static class FlexBase64 {

        private static final byte[] ENCODING_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(StandardCharsets.US_ASCII);

        /**
         * Creates a state driven base64 encoder.
         *
         * <p>The Encoder instance is not thread-safe, and must not be shared between threads without establishing a
         * happens-before relationship.</p>
         *
         * @param wrap whether or not to wrap at 76 characters with CRLF
         * @return an createEncoder instance
         */
        public static Encoder createEncoder(boolean wrap) {
            return new Encoder(wrap);
        }

        /**
         * Controls the encoding process.
         */
        public static final class Encoder {
            private int state;
            private int last;
            private int count;
            private final boolean wrap;
            private int lastPos;

            private Encoder(boolean wrap) {
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
            public void encode(ByteBuffer source, ByteBuffer target) {
                if (target == null) {
                    throw new IllegalStateException();
                }

                int last = this.last;
                int state = this.state;
                boolean wrap = this.wrap;
                int count = this.count;
                final byte[] encodingTable = FlexBase64.ENCODING_TABLE;

                int remaining = source.remaining();
                while (remaining > 0) {
                    // Unrolled state machine for performance (resumes and executes all states in one iteration)
                    int require = 4 - state;
                    require = wrap && (count >= 72) ? require + 2 : require;
                    if (target.remaining() < require) {
                        break;
                    }
                    //  ( 6 | 2) (4 | 4) (2 | 6)
                    int sourceByte = source.get() & 0xFF;
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
                this.lastPos = source.position();
            }

            /**
             * Completes an encoding session by writing out the necessary padding. This is essential to complying
             * with the Base64 format. This method will write at most 4 or 2 bytes, depending on whether or not wrapping
             * is enabled.
             *
             * @param target the byte buffer to write to
             */
            public void complete(ByteBuffer target) {
                if (state > 0) {
                    target.put(ENCODING_TABLE[last]);
                    for (int i = state; i < 3; i++) {
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
    public void testEncoderDecoderBuffer() throws IOException {
        byte[] nums = new byte[32768];
        for (int i = 0; i < 32768; i++) {
            nums[i] = (byte)(i % 255);
        }

        ByteBuffer source = ByteBuffer.wrap(nums);
        ByteBuffer target = ByteBuffer.allocate(65535);

        FlexBase64.Encoder encoder = FlexBase64.createEncoder(true);
        encoder.encode(source, target);
        encoder.complete(target);

        ByteBuffer decoded = ByteBuffer.allocate(nums.length);
        Base64Decoder decoder = new Base64Decoder();
        target.flip();
        decoder.decode(target, decoded);

        decoded.flip();

        Assert.assertEquals(nums.length, decoded.remaining());

        for (byte num : nums) {
            Assert.assertEquals(num, decoded.get());
        }
    }

    @Test
    public void testEncoderDecoderBufferLoops() throws IOException {
        byte[] nums = new byte[32768];
        for (int i = 0; i < 32768; i++) {
            nums[i] = (byte)(i % 255);
        }
        ByteBuffer source = ByteBuffer.wrap(nums);
        ByteBuffer target = ByteBuffer.allocate(65535);

        FlexBase64.Encoder encoder = FlexBase64.createEncoder(true);
        int limit = target.limit();
        target.limit(100);
        while (source.remaining() > 0) {
            encoder.encode(source, target);
            int add = limit - target.position();
            add = add < 100 ? add : 100;
            target.limit(target.limit() + add);
        }
        encoder.complete(target);

        ByteBuffer decoded = ByteBuffer.allocate(nums.length);
        Base64Decoder decoder = new Base64Decoder();
        target.flip();

        limit = decoded.limit();
        decoded.limit(100);
        while (target.remaining() > 0) {
            decoder.decode(target, decoded);
            int add = limit - decoded.position();
            add = add < 100 ? add : 100;
            decoded.limit(decoded.position() + add);
        }

        decoded.flip();

        Assert.assertEquals(nums.length, decoded.remaining());

        for (byte num : nums) {
            Assert.assertEquals(num, decoded.get());
        }
    }

}
