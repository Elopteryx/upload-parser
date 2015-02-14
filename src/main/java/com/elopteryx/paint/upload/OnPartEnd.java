package com.elopteryx.paint.upload;

import java.io.IOException;

/**
 * A functional interface. An implementation of it must be passed in the
 * {@link UploadParser#onPartEnd(OnPartEnd)} onPartEnd} method to call it at the end of parsing for each part.
 */
@FunctionalInterface
public interface OnPartEnd {

    /**
     * The consumer function to implement.
     * @param context The upload context
     * @throws IOException If an error occurred with the current channel
     */
    void onPartEnd(UploadContext context) throws IOException;
    
}
