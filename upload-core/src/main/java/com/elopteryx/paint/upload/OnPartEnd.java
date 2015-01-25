package com.elopteryx.paint.upload;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

@FunctionalInterface
public interface OnPartEnd {
    
    void accept(UploadContext context, WritableByteChannel channel) throws IOException;
    
}
