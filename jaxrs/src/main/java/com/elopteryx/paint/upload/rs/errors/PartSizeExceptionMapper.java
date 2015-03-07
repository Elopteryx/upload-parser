package com.elopteryx.paint.upload.rs.errors;

import com.elopteryx.paint.upload.errors.PartSizeException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class PartSizeExceptionMapper<T extends Throwable> implements ExceptionMapper<PartSizeException> {

    @Override
    public Response toResponse(PartSizeException exception) {
        return null;
    }
}
