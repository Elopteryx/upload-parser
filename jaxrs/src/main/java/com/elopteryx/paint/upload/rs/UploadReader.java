package com.elopteryx.paint.upload.rs;

import com.elopteryx.paint.upload.OnError;
import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.OnRequestComplete;
import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.errors.MultipartException;
import com.elopteryx.paint.upload.errors.RequestSizeException;
import com.elopteryx.paint.upload.impl.MultipartParser;
import com.elopteryx.paint.upload.impl.PartStreamHeaders;
import com.elopteryx.paint.upload.impl.UploadContextImpl;
import com.elopteryx.paint.upload.impl.UploadParser;

import javax.servlet.ServletException;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * This class is the base class for multipart message readers. Subclasses can further configure the parsing
 * process.
 */
@Provider
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class UploadReader<T> extends UploadParser<UploadReader<T>> implements MessageBodyReader<T>, OnPartBegin, OnPartEnd, OnRequestComplete, OnError {

    private static final String CONTENT_LENGTH = "Content-Length";

    private static final String CONTENT_ENCODING = "Content-Encoding";

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.equals(MediaType.MULTIPART_FORM_DATA_TYPE);
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {


        partBeginCallback = this;
        partEndCallback = this;

        //Fail fast mode
        if (maxRequestSize > -1) {
            long requestSize = Long.valueOf(httpHeaders.getFirst(CONTENT_LENGTH));
            if (requestSize > maxRequestSize)
                throw new RequestSizeException("The size of the request (" + requestSize +
                        ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
        }

        checkBuffer = ByteBuffer.allocate(sizeThreshold);
        context = new UploadContextImpl(null, uploadResponse);

        String mimeType = httpHeaders.getFirst(PartStreamHeaders.CONTENT_TYPE);
        String boundary;
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            boundary = PartStreamHeaders.extractBoundaryFromHeader(mimeType);
            if (boundary == null) {
                throw new RuntimeException("Could not find boundary in multipart request with ContentType: "+mimeType+", multipart data will not be available");
            }
            Charset charset = httpHeaders.getFirst(CONTENT_ENCODING) != null ? Charset.forName(httpHeaders.getFirst(CONTENT_ENCODING)) : ISO_8859_1;
            parseState = MultipartParser.beginParse(this, boundary.getBytes(), charset);
        }

        try {
            while(true) {
                int c = entityStream.read(buf);
                if (c == -1) {
                    if (!parseState.isComplete())
                        throw new MultipartException();
                    else
                        break;
                } else if(c > 0) {
                    checkRequestSize(c);
                    parseState.parse(ByteBuffer.wrap(buf, 0, c));
                }
            }
            onRequestComplete(context);
        } catch (Exception e) {
            onError(context, e);
        }
        return null;
    }

    @Override
    public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
        return null;
    }

    @Override
    public void onPartEnd(UploadContext context) throws IOException {

    }

    @Override
    public void onRequestComplete(UploadContext context) throws IOException, ServletException {

    }

    @Override
    public void onError(UploadContext context, Throwable throwable) {

    }
}
