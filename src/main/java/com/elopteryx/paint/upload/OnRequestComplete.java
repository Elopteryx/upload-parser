package com.elopteryx.paint.upload;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * A functional interface. An implementation of it must be passed in the
 * {@link UploadParser#onRequestComplete(OnRequestComplete)} onRequestComplete} method to call it after every part has been processed.
 */
public interface OnRequestComplete {

    /**
     * The consumer function to implement.
     * @param context The upload context
     * @throws IOException If an error occurs with the IO
     * @throws ServletException If and error occurred with the servlet
     */
    void onRequestComplete(UploadContext context) throws IOException, ServletException;
}
