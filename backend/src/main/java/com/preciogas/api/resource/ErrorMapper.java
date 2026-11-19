package com.preciogas.api.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Todos los errores salen como {"error": "mensaje"} con 400, 404 o 500,
 * según el contrato de la API.
 */
@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErrorMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        int status;
        String mensaje;
        if (e instanceof ApiException api) {
            status = api.getResponse().getStatus();
            mensaje = api.getMessage();
        } else if (e instanceof WebApplicationException wae) {
            status = wae.getResponse().getStatus();
            mensaje = switch (status) {
                case 404 -> "Ruta no encontrada";
                case 405 -> "Método no permitido";
                case 415 -> "Envía el cuerpo como application/json";
                default -> wae.getMessage();
            };
        } else if (e instanceof JsonProcessingException
                || e.getCause() instanceof JsonProcessingException) {
            status = 400;
            mensaje = "JSON no válido";
        } else {
            LOG.log(Level.SEVERE, "Error no controlado", e);
            status = 500;
            mensaje = "Error interno del servidor";
        }
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON + ";charset=UTF-8")
                .entity(Map.of("error", mensaje))
                .build();
    }
}
