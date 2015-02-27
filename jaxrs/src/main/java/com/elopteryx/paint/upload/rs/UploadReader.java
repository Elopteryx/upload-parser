package com.elopteryx.paint.upload.rs;

import com.elopteryx.paint.upload.UploadParser;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class UploadReader extends UploadParser {

    protected UploadReader(HttpServletRequest request) {
        super(request);
    }

    /**
     * Returns a parser implementation, allowing the caller to set configuration.
     * @param request The servlet request
     * @return A parser object
     * @throws ServletException If the parameters are invalid
     */
    @CheckReturnValue
    public static UploadParser newParser(@Nonnull HttpServletRequest request) throws ServletException {
        return UploadParser.newParser(request);
    }

    @Override
    public void setup() throws IOException {

    }
}
