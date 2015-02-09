package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.UploadParser;
import com.elopteryx.paint.upload.errors.PartSizeException;
import com.elopteryx.paint.upload.errors.RequestSizeException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

/**
 * Base class for the parser implementations. This holds the common methods, like the more specific
 * validation and the calling of the user-supplied functions.
 *
 * This class is only public to serve as an entry point in the implementation package, users
 * should not need to directly depend on this class.
 */
public abstract class UploadParserImpl extends UploadParser implements MultipartParser.PartHandler {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private static final int BUFFER_SIZE = 1024;

    private static final Charset defaultEncoding = StandardCharsets.ISO_8859_1;

    private ByteBuffer checkBuffer;

    private WritableByteChannel writableChannel;

    private long requestSize;

    protected ServletInputStream servletInputStream;

    protected UploadContextImpl context;

    protected MultipartParser.ParseState parseState;

    protected final byte[] buf = new byte[BUFFER_SIZE];

    public UploadParserImpl(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public void setup() throws IOException {
        requireNonNull(partBeginCallback, "Setting a valid part begin callback is mandatory!");
        requireNonNull(partEndCallback, "Setting a valid part end callback is mandatory!");

        //Fail fast mode
        if (maxRequestSize > -1) {
            long requestSize = request.getContentLengthLong();
            if (requestSize > maxRequestSize)
                throw new RequestSizeException("The size of the request (" + requestSize +
                        ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
        }

        checkBuffer = ByteBuffer.allocate(sizeThreshold);
        context = new UploadContextImpl(request, response);

        String mimeType = request.getHeader(PartStreamHeaders.CONTENT_TYPE);
        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = PartStreamHeaders.extractTokenFromHeader(mimeType, "boundary");
            if (boundary == null) {
                throw new RuntimeException("Could not find boundary in multipart request with ContentType: "+mimeType+", multipart data will not be available");
            }
            Charset charset = request.getCharacterEncoding() != null ? Charset.forName(request.getCharacterEncoding()) : defaultEncoding;
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), charset);
        }

        servletInputStream = request.getInputStream();
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     * @param additional The amount to add, always non negative
     */
    protected void checkPartSize(int additional) {
        long partSize = context.incrementAndGetPartBytesRead(additional);
        if (maxPartSize > -1 && partSize > maxPartSize)
            throw new PartSizeException("The size of the part (" + partSize +
                    ") is greater than the allowed size (" + maxPartSize + ")!", partSize, maxPartSize);
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     * @param additional The amount to add, always non negative
     */
    protected void checkRequestSize(int additional) {
        requestSize += additional;
        if (maxRequestSize > -1 && requestSize > maxRequestSize)
            throw new RequestSizeException("The size of the request (" + requestSize +
                    ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
    }

    @Override
    public void beginPart(final PartStreamHeaders headers) {
        final String disposition = headers.getHeader(PartStreamHeaders.CONTENT_DISPOSITION);
        if (disposition != null) {
            if (disposition.startsWith("form-data")) {
                String fieldName = PartStreamHeaders.extractQuotedValueFromHeader(disposition, "name");
                String fileName = PartStreamHeaders.extractQuotedValueFromHeader(disposition, "filename");
                context.reset(new PartStreamImpl(fileName, fieldName, headers.getHeader(PartStreamHeaders.CONTENT_TYPE), headers));
            }
        }
    }

    @Override
    public void data(final ByteBuffer buffer) throws IOException {
        checkPartSize(buffer.remaining());
        copyBuffer(buffer);
        if (context.isBuffering() && (context.getPartBytesRead() >= sizeThreshold)) {
            validate();
        }
        if(!context.isBuffering()) {
            while (buffer.hasRemaining()) {
                writableChannel.write(buffer);
            }
        }
    }

    private void copyBuffer(final ByteBuffer buffer) {
        int transferCount = Math.min(checkBuffer.remaining(), buffer.remaining());
        if (transferCount > 0) {
            checkBuffer.put(buffer.array(), buffer.arrayOffset() + buffer.position(), transferCount);
            buffer.position(buffer.position() + transferCount);
        }
    }

    private void validate() throws IOException {
        writableChannel = requireNonNull(partBeginCallback.onPartBegin(context, checkBuffer));
        context.setChannel(writableChannel);
        context.finishBuffering();
        checkBuffer.flip();
        while (checkBuffer.hasRemaining()) {
            writableChannel.write(checkBuffer);
        }
    }

    @Override
    public void endPart() throws IOException {
        if(context.isBuffering()) {
            validate();
        }
        checkBuffer.clear();
        context.updatePartBytesRead();
        partEndCallback.onPartEnd(context);
    }
}
