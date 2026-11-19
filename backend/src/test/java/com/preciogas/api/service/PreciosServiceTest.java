package com.preciogas.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.preciogas.api.dao.PreciosDao;
import com.preciogas.api.model.Competencia;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.MiPrecio;
import com.preciogas.api.model.Resumen;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PreciosServiceTest {

    // Centro por omisión de Config: 20.90, -101.30, radio 5 km.
    private static final double LAT = 20.90;
    private static final double LNG = -101.30;

    @Test
    void haversineDaCeroEnElMismoPuntoYUnGradoDeLatitudSonUnos111Km() {
        assertEquals(0.0, PreciosService.haversineKm(LAT, LNG, LAT, LNG), 1e-9);
        assertEquals(111.19, PreciosService.haversineKm(0, 0, 1, 0), 0.01);
    }

    @Test
    void competenciaFiltraPorRadioYCombustibleYOrdenaPorDistancia() {
        FakeDao dao = new FakeDao(List.of(
                est(1, 20.95, -101.30, Map.of("magna", 23.0)),            // ~5.6 km: fuera
                est(2, 20.92, -101.30, Map.of("magna", 24.0)),            // ~2.2 km
                est(3, 20.905, -101.30, Map.of("magna", 22.0)),           // ~0.6 km
                est(4, 20.91, -101.30, Map.of("diesel", 27.0))));         // sin magna

        Competencia c = new PreciosService(dao).competencia(LAT, LNG, 5, "magna");

        assertEquals(List.of(3L, 2L), c.estaciones().stream().map(Estacion::id).toList());
        assertTrue(c.estaciones().get(0).distanciaKm() < c.estaciones().get(1).distanciaKm());
    }

    @Test
    void competenciaSinCombustibleTraeTodasLasDelRadio() {
        FakeDao dao = new FakeDao(List.of(
                est(1, 20.905, -101.30, Map.of("magna", 22.0)),
                est(2, 20.91, -101.30, Map.of("diesel", 27.0))));

        assertEquals(2, new PreciosService(dao).competencia(LAT, LNG, 5, null).estaciones().size());
    }

    @Test
    void resumenCalculaPromedioMinMaxYDiferenciaContraMiPrecio() {
        FakeDao dao = new FakeDao(List.of(
                est(1, 20.905, -101.30, Map.of("magna", 23.00)),
                est(2, 20.91, -101.30, Map.of("magna", 24.00)),
                est(3, 20.92, -101.30, Map.of("magna", 23.50))));
        dao.misPrecios.add(new MiPrecio(1, "magna", 23.00, LocalDate.of(2026, 11, 1)));

        Resumen r = new PreciosService(dao).resumen("magna");

        assertEquals(23.50, r.promedio());
        assertEquals(23.00, r.minimo());
        assertEquals(24.00, r.maximo());
        assertEquals(23.00, r.miPrecio());
        assertEquals(-0.50, r.diferencia());
        assertEquals(3, r.estaciones());
    }

    @Test
    void resumenSinEstacionesNiMiPrecioDevuelveNulos() {
        Resumen r = new PreciosService(new FakeDao(List.of())).resumen("premium");

        assertNull(r.promedio());
        assertNull(r.minimo());
        assertNull(r.maximo());
        assertNull(r.miPrecio());
        assertNull(r.diferencia());
        assertEquals(0, r.estaciones());
    }

    private static Estacion est(long id, double lat, double lng, Map<String, Double> precios) {
        return new Estacion(id, "P" + id, "E" + id, "M", lat, lng, "X", null, precios);
    }

    /** DAO en memoria mínimo para probar el servicio sin depender de los datos de ejemplo. */
    private static final class FakeDao implements PreciosDao {
        final List<Estacion> estaciones;
        final List<MiPrecio> misPrecios = new ArrayList<>();

        FakeDao(List<Estacion> estaciones) {
            this.estaciones = estaciones;
        }

        @Override
        public List<Estacion> estacionesEnRectangulo(double latMin, double latMax, double lngMin, double lngMax) {
            return estaciones.stream()
                    .filter(e -> e.lat() >= latMin && e.lat() <= latMax && e.lng() >= lngMin && e.lng() <= lngMax)
                    .toList();
        }

        @Override
        public Instant ultimaActualizacion() {
            return Instant.EPOCH;
        }

        @Override
        public List<MiPrecio> misPrecios() {
            return misPrecios;
        }

        @Override
        public Optional<MiPrecio> miPrecioActual(String combustible) {
            return misPrecios.stream().filter(p -> p.combustible().equals(combustible)).findFirst();
        }

        @Override
        public MiPrecio agregarMiPrecio(String combustible, double precio) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<MiPrecio> corregirMiPrecio(long id, double precio) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String fuente() {
            return "prueba";
        }
    }
}
