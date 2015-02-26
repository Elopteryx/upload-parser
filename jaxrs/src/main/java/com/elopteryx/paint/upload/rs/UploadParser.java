package com.elopteryx.paint.upload.rs;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.AsyncResponse;
import java.io.IOException;

public class UploadParser extends com.elopteryx.paint.upload.UploadParser {

    protected UploadParser() {
        super(null, null);
    }

    /**
     * Returns a parser implementation, allowing the caller to set configuration.
     * @param request The servlet request
     * @param response The servlet response
     * @return A parser object
     * @throws ServletException If the parameters are invalid
     */
    @CheckReturnValue
    public static UploadParser newParser(@Nonnull HttpServletRequest request, @Nonnull AsyncResponse response)
            throws ServletException {
        if (!isMultipart(request))
            throw new ServletException("Not a multipart request!");
        return null;
    }

    @Override
    public void setup() throws IOException {

    }
}
