package com.elopteryx.paint.upload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * A functional interface. An implementation of it must be passed in the
 * UploadParser#onPartBegin method to call it at the start of parsing for each part.
 */
public interface OnPartBegin {

    /**
     * The function to implement. When it's called depends on the size threshold.
     * @param context The upload context
     * @param buffer The byte buffer containing the first bytes of the part
     * @return A non-null channel to write out the part
     * @throws IOException
     */
    WritableByteChannel apply(UploadContext context, ByteBuffer buffer) throws IOException;

}
