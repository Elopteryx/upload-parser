package com.elopteryx.paint.upload.rs.errors;

import com.elopteryx.paint.upload.errors.RequestSizeException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * An exception mapper which is called when the content length of the incoming upload request
 * is greater than the allowed maximum.
 */
@Provider
public class RequestSizeMapper implements ExceptionMapper<RequestSizeException> {

    @Override
    public Response toResponse(RequestSizeException exception) {
        System.out.println(exception.getActualSize());
        System.out.println(exception.getPermittedSize());
        return Response.serverError().status(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE).build();
    }
}
