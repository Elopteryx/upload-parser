package com.github.elopteryx.upload.rs;

import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.PartStream;
import com.github.elopteryx.upload.UploadContext;
import com.github.elopteryx.upload.internal.NullChannel;

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
