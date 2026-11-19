package com.preciogas.api.resource;

import com.preciogas.api.dao.Daos;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** GET /api/salud: para comprobar que la API está viva y qué datos usa. */
@Path("/salud")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class SaludResource {

    @GET
    public Map<String, Object> salud() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("estado", "ok");
        r.put("fuente", Daos.get().fuente());
        r.put("hora", Instant.now().toString());
        return r;
    }
}
