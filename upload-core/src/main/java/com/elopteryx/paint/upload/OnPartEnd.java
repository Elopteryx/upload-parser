package com.elopteryx.paint.upload;

import java.io.IOException;

/**
 * A functional interface. An implementation of it must be passed in the
 * UploadParser#onPartEnd method to call it at the end of parsing for each part.
 */
public interface OnPartEnd {

    /**
     * The consumer function to implement.
     * @param context The upload context
     * @throws IOException
     */
    void accept(UploadContext context) throws IOException;
    
}
