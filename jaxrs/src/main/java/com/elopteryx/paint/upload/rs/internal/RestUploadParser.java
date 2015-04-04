package com.elopteryx.paint.upload.rs.internal;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import com.elopteryx.paint.upload.PartStream;
import com.elopteryx.paint.upload.errors.MultipartException;
import com.elopteryx.paint.upload.errors.RequestSizeException;
import com.elopteryx.paint.upload.internal.BlockingUploadParser;
import com.elopteryx.paint.upload.internal.MultipartParser;
import com.elopteryx.paint.upload.internal.PartStreamHeaders;
import com.elopteryx.paint.upload.internal.PartStreamImpl;
import com.elopteryx.paint.upload.internal.UploadContextImpl;
import com.elopteryx.paint.upload.rs.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A subclass of the blocking parser. It doesn't have a dependency
 * on the servlet request and can be initialized from the header values.
 * This makes it ideal for a Jax-Rs environment, to be used in a
 * message body reader.
 */
public class RestUploadParser extends BlockingUploadParser {

    public RestUploadParser() {
        super(null);
    }

    /**
     * Initializes the parser from the given parameters and performs
     * a blocking parse.
     * @param contentLength The length of the request
     * @param mimeType The content type of the request
     * @param encoding The character encoding of the request
     * @param stream The request stream
     * @return The multipart object, representing the request
     * @throws IOException If an error occurred with the I/O
     */
    public MultiPartImpl doBlockingParse(long contentLength, String mimeType, String encoding, InputStream stream)
    throws IOException {
        if (maxRequestSize > -1) {
            if (contentLength > maxRequestSize) {
                throw new RequestSizeException("The size of the request ("
                        + contentLength
                        + ") is greater than the allowed size (" + maxRequestSize + ")!",
                        contentLength, maxRequestSize);
            }
        }

        checkBuffer = ByteBuffer.allocate(sizeThreshold);
        context = new UploadContextImpl(null, null);

        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = PartStreamHeaders.extractBoundaryFromHeader(mimeType);
            if (boundary == null) {
                throw new IllegalArgumentException("Could not find boundary in multipart request with ContentType: "
                        + mimeType
                        + ", multipart data will not be available");
            }
            Charset charset = encoding != null ? Charset.forName(encoding) : ISO_8859_1;
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), charset);

            inputStream = stream;
        }
        while (true) {
            int count = inputStream.read(buf);
            if (count == -1) {
                if (!parseState.isComplete()) {
                    throw new MultipartException("Stream ended unexpectedly!");
                } else {
                    break;
                }
            } else if (count > 0) {
                checkRequestSize(count);
                parseState.parse(ByteBuffer.wrap(buf, 0, count));
            }
        }
        List<Part> parts = new ArrayList<>();
        for (PartStream partStream : context.getPartStreams()) {
            parts.add(new PartImpl((PartStreamImpl)partStream));
        }
        return new MultiPartImpl(parts, requestSize);
    }
}
