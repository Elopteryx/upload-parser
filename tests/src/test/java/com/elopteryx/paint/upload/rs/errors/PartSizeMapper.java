package com.elopteryx.paint.upload.rs.errors;

import com.elopteryx.paint.upload.errors.PartSizeException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * An exception mapper which is called when the size of a part
 * is greater than the allowed maximum.
 */
@Provider
public class PartSizeMapper implements ExceptionMapper<PartSizeException> {

    @Override
    public Response toResponse(PartSizeException exception) {
        System.out.println(exception.getActualSize());
        System.out.println(exception.getPermittedSize());
        return Response.serverError().status(HttpServletResponse.SC_NOT_ACCEPTABLE).build();
    }
}
