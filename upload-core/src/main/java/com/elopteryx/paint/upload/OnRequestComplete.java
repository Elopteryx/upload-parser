package com.elopteryx.paint.upload;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * A functional interface. An implementation of it must be passed in the
 * UploadParser#onRequestComplete method to call it after every part has been processed.
 */
public interface OnRequestComplete {

    /**
     * The consumer function to implement.
     * @param context The upload context
     * @throws IOException
     */
    void accept(UploadContext context) throws IOException, ServletException;
}
