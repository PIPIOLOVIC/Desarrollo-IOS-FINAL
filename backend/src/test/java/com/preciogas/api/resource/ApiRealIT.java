package com.preciogas.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preciogas.api.config.ApiApplication;
import com.preciogas.api.config.BdPrueba;
import com.preciogas.api.config.Config;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Los endpoints de punta a punta (sin Tomcat) contra la base real.
 * Los precios propios que crea se borran al terminar cada prueba.
 */
class ApiRealIT extends JerseyTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final List<Long> creados = new ArrayList<>();

    @BeforeAll
    static void soloConBase() {
        BdPrueba.requiereBase();
    }

    @Override
    protected Application configure() {
        return new ApiApplication();
    }

    @BeforeEach
    void iniciar() throws Exception {
        super.setUp();
    }

    @AfterEach
    void terminar() throws Exception {
        for (long id : creados) {
            BdPrueba.ejecutar("DELETE FROM mi_precio WHERE id = ?", id);
        }
        super.tearDown();
    }

    private static JsonNode json(Response r) throws Exception {
        return JSON.readTree(r.readEntity(String.class));
    }

    private static Entity<String> cuerpo(String json) {
        return Entity.entity(json, MediaType.APPLICATION_JSON_TYPE);
    }

    private JsonNode get(String ruta, Object... query) throws Exception {
        var t = target(ruta);
        for (int i = 0; i < query.length; i += 2) {
            t = t.queryParam((String) query[i], query[i + 1]);
        }
        Response r = t.request().get();
        assertEquals(200, r.getStatus(), ruta);
        return json(r);
    }

    @Test
    void saludUsaLaBase() throws Exception {
        assertEquals("bd", get("salud").get("fuente").asText());
    }

    @Test
    void competenciaPorOmisionOrdenadaYDentroDelRadio() throws Exception {
        JsonNode r = get("competencia");
        Instant.parse(r.get("actualizado").asText());
        JsonNode estaciones = r.get("estaciones");
        assertFalse(estaciones.isEmpty(), "Sin competencia en el radio por omisión");

        double anterior = 0;
        for (JsonNode e : estaciones) {
            double km = e.get("distanciaKm").asDouble();
            assertTrue(km >= anterior, "No viene ordenada por distancia");
            assertTrue(km <= Config.radioPorOmision(), "Fuera del radio: " + km);
            assertNotEquals(Config.miPermiso(), e.get("permiso").asText(), "Mi gasolinera aparece como competencia");
            anterior = km;
        }
    }

    @Test
    void competenciaFiltraPorCombustibleYCreceConElRadio() throws Exception {
        for (JsonNode e : get("competencia", "combustible", "diesel").get("estaciones")) {
            assertTrue(e.get("precios").has("diesel"), "Sin diesel: " + e.get("permiso"));
        }
        int a5 = get("competencia", "radio", 5).get("estaciones").size();
        int a20 = get("competencia", "radio", 20).get("estaciones").size();
        int a100 = get("competencia", "radio", 100).get("estaciones").size();
        assertTrue(a5 <= a20 && a20 <= a100, a5 + " / " + a20 + " / " + a100);
        assertTrue(a100 > 100, "A 100 km debería haber cientos de estaciones: " + a100);
    }

    @Test
    void resumenCuadraConLaCompetencia() throws Exception {
        for (String c : List.of("magna", "premium", "diesel")) {
            JsonNode zona = get("competencia", "combustible", c).get("estaciones");
            JsonNode r = get("resumen", "combustible", c);

            assertEquals(zona.size(), r.get("estaciones").asInt(), c);
            double suma = 0, min = Double.MAX_VALUE, max = 0;
            for (JsonNode e : zona) {
                double p = e.get("precios").get(c).asDouble();
                suma += p;
                min = Math.min(min, p);
                max = Math.max(max, p);
            }
            assertEquals(Math.round(suma / zona.size() * 100) / 100.0, r.get("promedio").asDouble(), 0.006, c);
            assertEquals(min, r.get("minimo").asDouble(), 0.0001, c);
            assertEquals(max, r.get("maximo").asDouble(), 0.0001, c);
            if (!r.get("miPrecio").isNull()) {
                double esperada = Math.round((r.get("miPrecio").asDouble() - r.get("promedio").asDouble()) * 100) / 100.0;
                assertEquals(esperada, r.get("diferencia").asDouble(), 0.006, c);
            }
        }
    }

    @Test
    void misPreciosCrearCorregirYVerEnResumen() throws Exception {
        Response creado = target("mis-precios").request().post(cuerpo("{\"combustible\":\"premium\",\"precio\":29.29}"));
        assertEquals(201, creado.getStatus());
        JsonNode nuevo = json(creado);
        long id = nuevo.get("id").asLong();
        creados.add(id);
        assertEquals(29.29, nuevo.get("precio").asDouble(), 0.0001);

        assertEquals(id, get("mis-precios").get(0).get("id").asLong(), "El nuevo debe salir primero");
        assertEquals(29.29, get("resumen", "combustible", "premium").get("miPrecio").asDouble(), 0.0001);

        Response corregido = target("mis-precios/" + id).request().put(cuerpo("{\"precio\":29.39}"));
        assertEquals(200, corregido.getStatus());
        assertEquals(29.39, json(corregido).get("precio").asDouble(), 0.0001);
        assertEquals(29.39, get("resumen", "combustible", "premium").get("miPrecio").asDouble(), 0.0001);

        assertEquals(404, target("mis-precios/999999999").request().put(cuerpo("{\"precio\":20}")).getStatus());
        assertEquals(400, target("mis-precios").request().post(cuerpo("{\"combustible\":\"gas\",\"precio\":20}")).getStatus());
    }

    @Test
    void adminPideElToken() {
        assertEquals(401, target("admin/actualizar").request().post(Entity.text("")).getStatus());
        assertEquals(401, target("admin/actualizar").request().header("X-Admin-Token", "token-equivocado")
                .post(Entity.text("")).getStatus());
    }
}
