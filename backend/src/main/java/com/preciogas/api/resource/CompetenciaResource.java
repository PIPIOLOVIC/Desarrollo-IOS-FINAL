package com.preciogas.api.resource;

import com.preciogas.api.config.Config;
import com.preciogas.api.dao.Daos;
import com.preciogas.api.model.Combustible;
import com.preciogas.api.model.Competencia;
import com.preciogas.api.service.PreciosService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * GET /api/competencia?lat=&lng=&radio=&combustible=
 * Todos los parámetros son opcionales: sin lat/lng usa la ubicación de mi gasolinera,
 * sin radio usa 5 km y sin combustible devuelve todas las estaciones con todos sus precios.
 */
@Path("/competencia")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class CompetenciaResource {

    private final PreciosService service = new PreciosService(Daos.get());

    @GET
    public Competencia competencia(@QueryParam("lat") String lat,
                                   @QueryParam("lng") String lng,
                                   @QueryParam("radio") String radio,
                                   @QueryParam("combustible") String combustible) {
        double la = numero("lat", lat, Config.miLat());
        double ln = numero("lng", lng, Config.miLng());
        double r = numero("radio", radio, Config.radioPorOmision());
        if (la < -90 || la > 90 || ln < -180 || ln > 180) {
            throw ApiException.badRequest("Coordenadas fuera de rango");
        }
        if (r <= 0 || r > 100) {
            throw ApiException.badRequest("El radio debe estar entre 0 y 100 km");
        }
        String c = (combustible == null || combustible.isBlank()) ? null : Combustible.validar(combustible);
        return service.competencia(la, ln, r, c);
    }

    /** Parámetros como texto para responder 400 con nuestro formato si no son números. */
    private static double numero(String nombre, String valor, double porOmision) {
        if (valor == null || valor.isBlank()) {
            return porOmision;
        }
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("El parámetro " + nombre + " debe ser un número");
        }
    }
}
