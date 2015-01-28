package com.elopteryx.paint.upload;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * A functional interface. An implementation of it must be passed in the
 * UploadParser#onPartEnd method to call it at the end of parsing for each part.
 */
public interface OnPartEnd {

    /**
     * The consumer function to implement.
     * @param context The upload context
     * @param channel The channel which was returned by the onPartBegin method for the current part
     * @throws IOException
     */
    void accept(UploadContext context, WritableByteChannel channel) throws IOException;
    
}
