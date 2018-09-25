package com.github.elopteryx.upload.internal.integration;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.nio.ByteBuffer;

class RequestBuilder {

    private static final byte[] LINE_FEED = "\r\n".getBytes(ISO_8859_1);

    private final ByteBuffer buffer;

    private final byte[] boundary;

    static RequestBuilder newBuilder(String boundary) {
        return new RequestBuilder(boundary);
    }

    private RequestBuilder(String boundary) {
        this.buffer = ByteBuffer.allocate(300_000);
        this.boundary = boundary.getBytes(ISO_8859_1);
    }

    RequestBuilder addFormField(String name, String value) {
        buffer.put(LINE_FEED);
        buffer.put("--".getBytes(ISO_8859_1)).put(boundary).put(LINE_FEED);
        buffer.put("Content-Disposition: form-data; name=\"".getBytes(ISO_8859_1)).put(name.getBytes(ISO_8859_1)).put("\"".getBytes(ISO_8859_1)).put(LINE_FEED);
        buffer.put("Content-Type: text/plain; charset=".getBytes(ISO_8859_1)).put(ISO_8859_1.name().getBytes(ISO_8859_1)).put(LINE_FEED);
        buffer.put(LINE_FEED);
        buffer.put(value.getBytes(ISO_8859_1));

        return this;
    }

    RequestBuilder addFilePart(String fieldName, byte[] content, String contentType, String fileName) {
        buffer.put(LINE_FEED);
        buffer.put("--".getBytes(ISO_8859_1)).put(boundary).put(LINE_FEED);
        buffer.put("Content-Disposition: form-data; name=\"".getBytes(ISO_8859_1)).put(fieldName.getBytes(ISO_8859_1)).put("\"; filename=\"".getBytes(ISO_8859_1)).put(fileName.getBytes(ISO_8859_1)).put("\"".getBytes(ISO_8859_1)).put(LINE_FEED);
        buffer.put("Content-Type: ".getBytes(ISO_8859_1)).put(contentType.getBytes(ISO_8859_1)).put(LINE_FEED);
        buffer.put(LINE_FEED);
        buffer.put(content);

        return this;
    }

    ByteBuffer finish() {
        buffer.put(LINE_FEED);
        buffer.put("--".getBytes(ISO_8859_1)).put(boundary).put("--".getBytes(ISO_8859_1)).put(LINE_FEED);

        buffer.flip();
        return buffer;
    }

}
