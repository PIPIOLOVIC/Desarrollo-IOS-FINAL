package com.preciogas.api.resource;

import com.preciogas.api.collector.Colector;
import com.preciogas.api.config.Config;
import com.preciogas.api.model.ResultadoActualizacion;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * POST /api/admin/actualizar: corre el colector de la CNE a mano (pruebas y demo).
 * Si admin.token está configurado, hay que mandar el encabezado X-Admin-Token.
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class AdminResource {

    @POST
    @Path("/actualizar")
    public ResultadoActualizacion actualizar(@HeaderParam("X-Admin-Token") String token) {
        String esperado = Config.adminToken();
        if (esperado != null && !iguales(esperado, token)) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "Falta o no es válido el encabezado X-Admin-Token");
        }
        return Colector.ejecutar();
    }

    /** Comparación en tiempo constante para no filtrar el token por tiempos de respuesta. */
    private static boolean iguales(String a, String b) {
        if (b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
