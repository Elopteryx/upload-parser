package com.elopteryx.paint.upload;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public interface OnPartEnd {
    
    void accept(UploadContext context, WritableByteChannel channel) throws IOException;
    
}
