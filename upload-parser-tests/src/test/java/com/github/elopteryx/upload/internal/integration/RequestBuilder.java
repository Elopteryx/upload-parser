package com.github.elopteryx.upload.internal.integration;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.nio.ByteBuffer;

final class RequestBuilder {

    private static final byte[] LINE_FEED = "\r\n".getBytes(ISO_8859_1);

    private final ByteBuffer buffer;

    private final String boundary;

    static RequestBuilder newBuilder(final String boundary) {
        return new RequestBuilder(boundary);
    }

    private RequestBuilder(final String boundary) {
        this.buffer = ByteBuffer.allocate(300_000);
        this.boundary = boundary;
    }

    RequestBuilder addFormField(final String name, final String value) {
        buffer.put(LINE_FEED);
        buffer.put(("--" + boundary).getBytes(ISO_8859_1));
        buffer.put(LINE_FEED);
        buffer.put(("Content-Disposition: form-data; name=\"" + name + "\"").getBytes(ISO_8859_1));
        buffer.put(LINE_FEED);
        buffer.put(("Content-Type: text/plain; charset=" + ISO_8859_1.name()).getBytes(ISO_8859_1));
        buffer.put(LINE_FEED);
        buffer.put(LINE_FEED);
        buffer.put(value.getBytes(ISO_8859_1));

        return this;
    }

    RequestBuilder addFilePart(final String fieldName, final byte[] content, final String contentType, final String fileName) {
        buffer.put(LINE_FEED);
        buffer.put(("--" + boundary).getBytes(ISO_8859_1));
        buffer.put(LINE_FEED);
        buffer.put(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"").getBytes(ISO_8859_1));
        buffer.put(LINE_FEED);
        buffer.put(("Content-Type: " + contentType).getBytes(ISO_8859_1));
        buffer.put(LINE_FEED);
        buffer.put(LINE_FEED);
        buffer.put(content);

        return this;
    }

    ByteBuffer finish() {
        buffer.put(LINE_FEED);
        buffer.put(("--" + boundary + "--").getBytes(ISO_8859_1));
        buffer.put(LINE_FEED);

        buffer.flip();
        return buffer;
    }

}
