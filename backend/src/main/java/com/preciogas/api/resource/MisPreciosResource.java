package com.preciogas.api.resource;

import com.preciogas.api.dao.Daos;
import com.preciogas.api.dao.PreciosDao;
import com.preciogas.api.model.Combustible;
import com.preciogas.api.model.MiPrecio;
import com.preciogas.api.model.Peticiones.CorreccionPrecio;
import com.preciogas.api.model.Peticiones.NuevoPrecio;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

/**
 * GET  /api/mis-precios       lista actual e historial (más reciente primero)
 * POST /api/mis-precios       registra un precio nuevo  -> 201
 * PUT  /api/mis-precios/{id}  corrige un precio         -> 200
 */
@Path("/mis-precios")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON)
public class MisPreciosResource {

    private final PreciosDao dao = Daos.get();

    @GET
    public List<MiPrecio> lista() {
        return dao.misPrecios();
    }

    @POST
    public Response registrar(NuevoPrecio cuerpo, @Context UriInfo uri) {
        if (cuerpo == null) {
            throw ApiException.badRequest("Falta el cuerpo JSON");
        }
        String combustible = Combustible.validar(cuerpo.combustible());
        double precio = validarPrecio(cuerpo.precio());
        MiPrecio creado = dao.agregarMiPrecio(combustible, precio);
        return Response.created(uri.getAbsolutePathBuilder().path(String.valueOf(creado.id())).build())
                .entity(creado)
                .build();
    }

    @PUT
    @Path("/{id}")
    public MiPrecio corregir(@PathParam("id") String id, CorreccionPrecio cuerpo) {
        long n;
        try {
            n = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("El id debe ser un número");
        }
        if (cuerpo == null) {
            throw ApiException.badRequest("Falta el cuerpo JSON");
        }
        double precio = validarPrecio(cuerpo.precio());
        return dao.corregirMiPrecio(n, precio)
                .orElseThrow(() -> ApiException.notFound("No existe el precio con id " + n));
    }

    private static double validarPrecio(Double precio) {
        if (precio == null) {
            throw ApiException.badRequest("Falta el precio");
        }
        if (precio.isNaN() || precio <= 0 || precio >= 100) {
            throw ApiException.badRequest("El precio debe ser mayor que 0 y menor que 100 pesos por litro");
        }
        return Math.round(precio * 100.0) / 100.0;
    }
}
