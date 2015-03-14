package com.elopteryx.paint.upload.rs;

import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.PartStream;
import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.impl.NullChannel;

import javax.annotation.Nonnull;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.nio.ByteBuffer;

@Provider
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class CustomUploadReader extends UploadReader {

    @Nonnull
    @Override
    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
        return PartOutput.from(new NullChannel());
    }

    @Override
    public void onPartEnd(UploadContext context) throws IOException {
        PartStream part = context.getCurrentPart();
        System.out.println(part.getSubmittedFileName());
    }
}
