package com.preciogas.api.resource;

import com.preciogas.api.collector.Colector;
import com.preciogas.api.config.Config;
import com.preciogas.api.model.ResultadoActualizacion;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * POST /api/admin/actualizar: corre el colector de la CNE a mano (pruebas y demo).
 * Con base de datos configurada, admin.token es obligatorio (si falta: 500).
 * Si admin.token está configurado, hay que mandar el encabezado X-Admin-Token.
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class AdminResource {

    @POST
    @Path("/actualizar")
    public ResultadoActualizacion actualizar(@HeaderParam("X-Admin-Token") String token,
                                             @QueryParam("origen") String origen) {
        String esperado = Config.adminToken();
        // Con base real el endpoint descarga y escribe: no se deja abierto sin token.
        if (esperado == null && Config.hayBaseDeDatos()) {
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, "Configura admin.token para usar este endpoint");
        }
        if (esperado != null && !iguales(esperado, token)) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "Falta o no es válido el encabezado X-Admin-Token");
        }
        // origen=archivo: no descarga, usa el archivo de respaldo del servidor
        if (origen != null && !origen.equals("archivo")) {
            throw ApiException.badRequest("origen no válido: " + origen + " (usa origen=archivo o no lo mandes)");
        }
        return Colector.ejecutar(origen != null);
    }

    /** Comparación en tiempo constante para no filtrar el token por tiempos de respuesta. */
    private static boolean iguales(String a, String b) {
        if (b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
