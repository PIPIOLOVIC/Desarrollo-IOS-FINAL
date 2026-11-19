package com.preciogas.api.resource;

import com.preciogas.api.dao.Daos;
import com.preciogas.api.model.Combustible;
import com.preciogas.api.model.Resumen;
import com.preciogas.api.service.PreciosService;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** GET /api/resumen?combustible=magna (magna si no se indica). */
@Path("/resumen")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class ResumenResource {

    private final PreciosService service = new PreciosService(Daos.get());

    @GET
    public Resumen resumen(@QueryParam("combustible") @DefaultValue("magna") String combustible) {
        return service.resumen(Combustible.validar(combustible));
    }
}
