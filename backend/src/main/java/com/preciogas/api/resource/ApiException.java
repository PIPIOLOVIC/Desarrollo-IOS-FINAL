package com.preciogas.api.resource;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Error con código HTTP; ErrorMapper lo convierte en {"error": "mensaje"}. */
public class ApiException extends WebApplicationException {

    public ApiException(Response.Status status, String mensaje) {
        super(mensaje, status);
    }

    public static ApiException badRequest(String mensaje) {
        return new ApiException(Response.Status.BAD_REQUEST, mensaje);
    }

    public static ApiException notFound(String mensaje) {
        return new ApiException(Response.Status.NOT_FOUND, mensaje);
    }
}
