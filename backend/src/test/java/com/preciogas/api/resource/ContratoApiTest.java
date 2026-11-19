package com.preciogas.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preciogas.api.config.ApiApplication;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba de punta a punta (sin Tomcat) de que cada endpoint responde con la forma
 * que dice el contrato de la API: rutas, códigos HTTP, nombres de campos y errores.
 */
class ContratoApiTest extends JerseyTest {

    private static final ObjectMapper JSON = new ObjectMapper();

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
        super.tearDown();
    }

    private static JsonNode json(Response r) throws Exception {
        String cuerpo = r.readEntity(String.class);
        assertTrue(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE), "No es JSON: " + cuerpo);
        return JSON.readTree(cuerpo);
    }

    private static Entity<String> cuerpo(String json) {
        return Entity.entity(json, MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    void salud() throws Exception {
        Response r = target("salud").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("ok", json(r).get("estado").asText());
    }

    @Test
    void competenciaTieneLaFormaDelContrato() throws Exception {
        Response r = target("competencia")
                .queryParam("lat", 20.90).queryParam("lng", -101.30)
                .queryParam("radio", 5).queryParam("combustible", "magna")
                .request().get();
        assertEquals(200, r.getStatus());
        JsonNode body = json(r);

        Instant.parse(body.get("actualizado").asText()); // ISO 8601
        JsonNode estaciones = body.get("estaciones");
        assertTrue(estaciones.isArray() && estaciones.size() > 0);

        double anterior = -1;
        for (JsonNode e : estaciones) {
            for (String campo : List.of("id", "permiso", "nombre", "marca", "lat", "lng", "distanciaKm", "precios")) {
                assertTrue(e.has(campo), "Falta " + campo);
            }
            assertFalse(e.has("municipio"), "municipio no es parte del contrato");
            assertTrue(e.get("precios").has("magna"));
            double d = e.get("distanciaKm").asDouble();
            assertTrue(d <= 5 && d >= anterior, "Debe ir ordenada por distancia y dentro del radio");
            anterior = d;
        }
    }

    @Test
    void resumenTieneLaFormaDelContrato() throws Exception {
        Response r = target("resumen").queryParam("combustible", "magna").request().get();
        assertEquals(200, r.getStatus());
        JsonNode body = json(r);
        assertEquals("magna", body.get("combustible").asText());
        for (String campo : List.of("promedio", "minimo", "maximo", "miPrecio", "diferencia", "estaciones")) {
            assertTrue(body.has(campo), "Falta " + campo);
        }
        assertTrue(body.get("minimo").asDouble() <= body.get("promedio").asDouble());
        assertTrue(body.get("promedio").asDouble() <= body.get("maximo").asDouble());
    }

    @Test
    void misPreciosCrearListarYCorregir() throws Exception {
        Response creado = target("mis-precios").request()
                .post(cuerpo("{\"combustible\":\"Diesel\",\"precio\":27.456,\"extra\":true}"));
        assertEquals(201, creado.getStatus());
        assertNotNull(creado.getLocation());
        JsonNode nuevo = json(creado);
        assertEquals("diesel", nuevo.get("combustible").asText());
        assertEquals(27.46, nuevo.get("precio").asDouble());
        LocalDate.parse(nuevo.get("fecha").asText()); // ISO 8601, solo fecha
        long id = nuevo.get("id").asLong();

        JsonNode lista = json(target("mis-precios").request().get());
        assertTrue(lista.isArray());
        assertEquals(id, lista.get(0).get("id").asLong(), "El más reciente va primero");

        Response corregido = target("mis-precios/" + id).request().put(cuerpo("{\"precio\":27.10}"));
        assertEquals(200, corregido.getStatus());
        JsonNode c = json(corregido);
        assertEquals(id, c.get("id").asLong());
        assertEquals(27.10, c.get("precio").asDouble());
    }

    @Test
    void adminActualizarDevuelveConteos() throws Exception {
        Response r = target("admin/actualizar").request().post(Entity.text(""));
        assertEquals(200, r.getStatus());
        JsonNode body = json(r);
        assertTrue(body.get("estaciones").asInt() > 0);
        assertTrue(body.get("precios").asInt() > 0);
    }

    @Test
    void erroresSalenComoJsonConElCodigoCorrecto() throws Exception {
        assertError(target("resumen").queryParam("combustible", "gas").request().get(), 400);
        assertError(target("competencia").queryParam("lat", "abc").request().get(), 400);
        assertError(target("competencia").queryParam("radio", 0).request().get(), 400);
        assertError(target("mis-precios").request().post(cuerpo("{\"combustible\":\"magna\"}")), 400);
        assertError(target("mis-precios").request().post(cuerpo("{\"combustible\":\"magna\",\"precio\":-1}")), 400);
        assertError(target("mis-precios").request().post(cuerpo("{\"combustible\":")), 400);
        assertError(target("mis-precios/abc").request().put(cuerpo("{\"precio\":20}")), 400);
        assertError(target("mis-precios/999999").request().put(cuerpo("{\"precio\":20}")), 404);
        assertError(target("no-existe").request().get(), 404);
        assertError(target("admin/actualizar").request().get(), 405);
    }

    private static void assertError(Response r, int status) throws Exception {
        assertEquals(status, r.getStatus());
        JsonNode body = json(r);
        assertTrue(body.hasNonNull("error") && !body.get("error").asText().isBlank());
    }
}
