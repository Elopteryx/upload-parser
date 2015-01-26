package com.elopteryx.paint.upload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public interface OnPartBegin {
    
    WritableByteChannel apply(UploadContext context, ByteBuffer buffer) throws IOException;

}
