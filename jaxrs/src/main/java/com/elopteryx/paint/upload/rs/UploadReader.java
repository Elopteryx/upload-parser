package com.elopteryx.paint.upload.rs;

import com.elopteryx.paint.upload.OnError;
import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.OnRequestComplete;
import com.elopteryx.paint.upload.PartOutput;
import com.elopteryx.paint.upload.UploadContext;

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

/**
 * This class is the base class for multipart message readers. Subclasses can further configure the parsing
 * process.
 */
@Provider
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class UploadReader<T> implements MessageBodyReader<T>, OnPartBegin, OnPartEnd, OnRequestComplete, OnError {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.equals(MediaType.MULTIPART_FORM_DATA_TYPE);
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        //Parsing with InputStream only? Refactor...
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
