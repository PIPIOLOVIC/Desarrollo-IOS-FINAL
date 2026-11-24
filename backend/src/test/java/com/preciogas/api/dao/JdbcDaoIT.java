package com.preciogas.api.dao;

import static com.preciogas.api.config.BdPrueba.numero;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.preciogas.api.config.BdPrueba;
import com.preciogas.api.config.Config;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.MiPrecio;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** JdbcDao contra la base real. Los precios propios que crea se borran al terminar cada prueba. */
class JdbcDaoIT {

    private static final Set<String> COMBUSTIBLES = Set.of("magna", "premium", "diesel");

    private final JdbcDao dao = new JdbcDao();
    private final List<Long> creados = new ArrayList<>();

    @BeforeAll
    static void soloConBase() {
        BdPrueba.requiereBase();
    }

    @AfterEach
    void borrarLoCreado() throws Exception {
        for (long id : creados) {
            BdPrueba.ejecutar("DELETE FROM mi_precio WHERE id = ?", id);
        }
    }

    private List<Estacion> alrededorDeMi(double grados) {
        return dao.estacionesEnRectangulo(Config.miLat() - grados, Config.miLat() + grados,
                Config.miLng() - grados, Config.miLng() + grados);
    }

    @Test
    void estacionesEnRectanguloTraeEstacionesDentroConPreciosValidos() {
        double g = 0.1;
        List<Estacion> estaciones = alrededorDeMi(g);
        assertFalse(estaciones.isEmpty(), "No hay estaciones a ~10 km de mi gasolinera");

        Set<Long> ids = new HashSet<>();
        for (Estacion e : estaciones) {
            assertTrue(ids.add(e.id()), "Estación repetida: " + e.id());
            assertTrue(e.lat() >= Config.miLat() - g && e.lat() <= Config.miLat() + g, "lat fuera: " + e);
            assertTrue(e.lng() >= Config.miLng() - g && e.lng() <= Config.miLng() + g, "lng fuera: " + e);
            assertNotNull(e.permiso());
            assertNotNull(e.nombre());
            assertFalse(e.precios().isEmpty(), "Estación sin precios: " + e.permiso());
            for (Map.Entry<String, Double> p : e.precios().entrySet()) {
                assertTrue(COMBUSTIBLES.contains(p.getKey()), "Combustible raro: " + p.getKey());
                assertTrue(p.getValue() >= 15 && p.getValue() <= 40, "Precio raro: " + p);
            }
        }
    }

    @Test
    void cadaPrecioEsElMasRecienteDeEsaEstacion() throws Exception {
        List<Estacion> estaciones = alrededorDeMi(0.1);
        for (Estacion e : estaciones.subList(0, Math.min(5, estaciones.size()))) {
            for (Map.Entry<String, Double> p : e.precios().entrySet()) {
                Double esperado = numero("SELECT precio FROM precio WHERE estacion_id = ? AND combustible = ? "
                        + "ORDER BY fecha DESC LIMIT 1", e.id(), p.getKey());
                assertEquals(esperado, p.getValue(), 0.001, e.permiso() + " " + p.getKey());
            }
        }
    }

    @Test
    void rectanguloSinEstacionesDevuelveListaVacia() {
        assertTrue(dao.estacionesEnRectangulo(-10.5, -10.4, -10.5, -10.4).isEmpty()); // en el océano Atlántico
    }

    @Test
    void ultimaActualizacionExisteYNoEsDelFuturo() {
        Instant u = dao.ultimaActualizacion();
        assertNotNull(u);
        assertTrue(u.isBefore(Instant.now().plusSeconds(60)), "actualizado en el futuro (¿zona horaria?): " + u);
    }

    @Test
    void agregarRedondeaPoneFechaDeHoyYEsElActual() {
        MiPrecio nuevo = dao.agregarMiPrecio("premium", 29.456);
        creados.add(nuevo.id());

        assertTrue(nuevo.id() > 0);
        assertEquals(29.46, nuevo.precio(), 0.0001);
        assertEquals(LocalDate.now(Config.zona()), nuevo.fecha());
        assertEquals(nuevo, dao.miPrecioActual("premium").orElseThrow());
        assertEquals(nuevo, dao.misPrecios().get(0), "misPrecios debe empezar por el más reciente");
    }

    @Test
    void corregirCambiaElPrecioYRespetaLaFecha() throws Exception {
        MiPrecio nuevo = dao.agregarMiPrecio("diesel", 26.80);
        creados.add(nuevo.id());

        MiPrecio corregido = dao.corregirMiPrecio(nuevo.id(), 26.95).orElseThrow();
        assertEquals(26.95, corregido.precio(), 0.0001);
        assertEquals(nuevo.fecha(), corregido.fecha());
        assertEquals(26.95, numero("SELECT precio FROM mi_precio WHERE id = ?", nuevo.id()), 0.0001);

        // Mismo valor otra vez: MySQL cuenta 0 filas cambiadas, pero el registro existe.
        assertTrue(dao.corregirMiPrecio(nuevo.id(), 26.95).isPresent());
        assertTrue(dao.corregirMiPrecio(-1, 20).isEmpty());
    }

    @Test
    void fuenteEsBd() {
        assertEquals("bd", dao.fuente());
        assertTrue(Daos.get() instanceof JdbcDao, "Con base configurada Daos debe usar JdbcDao");
    }
}
