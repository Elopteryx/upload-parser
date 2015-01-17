package com.elopteryx.paint.upload;

import javax.servlet.ServletException;
import java.io.IOException;

@FunctionalInterface
public interface OnRequestComplete {

    void accept(UploadContext context) throws IOException, ServletException;
}
