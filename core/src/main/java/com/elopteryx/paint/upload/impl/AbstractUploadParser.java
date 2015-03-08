/*
 * Copyright (C) 2015 Adam Forgacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.UploadParser;
import com.elopteryx.paint.upload.errors.PartSizeException;
import com.elopteryx.paint.upload.errors.RequestSizeException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;

/**
 * Base class for the parser implementations. This holds the common methods, like the more specific
 * validation and the calling of the user-supplied functions.
 */
public abstract class AbstractUploadParser<T extends AbstractUploadParser<T>> extends UploadParser<T> implements MultipartParser.PartHandler {

    /**
     * The valid mime type.
     */
    protected static final String MULTIPART_FORM_DATA = "multipart/form-data";

    /**
     * The buffer that stores the first bytes of the current part.
     */
    protected ByteBuffer checkBuffer;

    /**
     * The channel to where the current part is written.
     */
    protected WritableByteChannel writableChannel;

    /**
     * The known size of the request.
     */
    protected long requestSize;

    /**
     * The context instance.
     */
    protected UploadContextImpl context;

    /**
     * The reference to the multipart parser.
     */
    protected MultipartParser.ParseState parseState;

    /**
     * The buffer that stores the bytes which were read from the
     * servlet input stream or from a different source.
     */
    protected final byte[] buf = new byte[1024];

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
        if (disposition != null && disposition.startsWith("form-data")) {
            String fieldName = PartStreamHeaders.extractQuotedValueFromHeader(disposition, "name");
            String fileName = PartStreamHeaders.extractQuotedValueFromHeader(disposition, "filename");
            context.reset(new PartStreamImpl(fileName, fieldName, headers));
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
        PartOutput output = null;
        if(partBeginCallback != null) {
            output = requireNonNull(partBeginCallback.onPartBegin(context, checkBuffer));
            if (output.safeToCast(WritableByteChannel.class))
                writableChannel = output.unwrap(WritableByteChannel.class);
            else if (output.safeToCast(OutputStream.class))
                writableChannel = Channels.newChannel(output.unwrap(OutputStream.class));
            else if (output.safeToCast(Path.class))
                writableChannel = Files.newByteChannel(output.unwrap(Path.class), EnumSet.of(CREATE, TRUNCATE_EXISTING, WRITE));
            else
                throw new IllegalArgumentException("Invalid output object!");
        }
        if(output == null) {
            writableChannel = new NullChannel();
            output = PartOutput.from(writableChannel);
        }
        context.setOutput(output);
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
        writableChannel.close();
        if(partEndCallback != null)
            partEndCallback.onPartEnd(context);
    }
}
