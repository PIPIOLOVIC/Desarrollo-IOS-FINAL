package com.preciogas.api.dao;

import com.preciogas.api.config.Config;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.MiPrecio;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Datos de ejemplo en memoria (NO son datos reales) para que el equipo SwiftUI
 * pueda trabajar mientras se termina la base de datos y el colector de la CNE.
 * Los cambios en mis-precios se pierden al reiniciar Tomcat.
 */
public class EjemploDao implements PreciosDao {

    private static final Instant ACTUALIZADO = Instant.parse("2026-11-17T06:00:00Z");

    private final List<Estacion> estaciones = List.of(
            est(101, "PL/0001/EXP/ES/2015", "Gasolinera Ejemplo Centro", "Marca A", 20.910, -101.290, 23.69, 28.49, 27.15),
            est(102, "PL/0002/EXP/ES/2015", "Gasolinera Ejemplo Norte", "Marca B", 20.925, -101.305, 23.89, 28.79, 27.35),
            est(103, "PL/0003/EXP/ES/2016", "Gasolinera Ejemplo Sur", "Marca A", 20.880, -101.310, 22.90, 27.95, 26.80),
            est(104, "PL/0004/EXP/ES/2016", "Gasolinera Ejemplo Oriente", "Marca C", 20.902, -101.270, 24.10, 28.99, 27.60),
            est(105, "PL/0005/EXP/ES/2017", "Gasolinera Ejemplo Poniente", "Marca D", 20.895, -101.330, 23.45, 28.20, null),
            est(106, "PL/0006/EXP/ES/2017", "Gasolinera Ejemplo Bulevar", "Marca B", 20.915, -101.320, 23.75, 28.55, 27.20),
            est(107, "PL/0007/EXP/ES/2018", "Gasolinera Ejemplo Libramiento", "Marca C", 20.870, -101.280, 24.60, 29.10, 27.90),
            est(108, "PL/0008/EXP/ES/2018", "Gasolinera Ejemplo Mercado", "Marca A", 20.905, -101.298, 23.55, null, 26.95),
            est(109, "PL/0009/EXP/ES/2019", "Gasolinera Ejemplo Estadio", "Marca D", 20.932, -101.285, 23.99, 28.70, 27.40),
            est(110, "PL/0010/EXP/ES/2019", "Gasolinera Ejemplo Carretera", "Marca B", 20.860, -101.345, 23.30, 28.15, 26.70),
            est(111, "PL/0011/EXP/ES/2020", "Gasolinera Ejemplo Industrial", "Marca C", 20.940, -101.340, 23.80, 28.60, 27.10),
            est(112, "PL/0012/EXP/ES/2020", "Gasolinera Ejemplo Aeropuerto", "Marca A", 20.990, -101.250, 24.20, 29.30, 27.80),
            est(113, "PL/0013/EXP/ES/2021", "Gasolinera Ejemplo Salida", "Marca D", 20.820, -101.380, 22.70, 27.60, 26.40));

    private final List<MiPrecio> misPrecios = new ArrayList<>();
    private final AtomicLong siguienteId = new AtomicLong(1);

    public EjemploDao() {
        LocalDate antes = LocalDate.of(2026, 11, 10);
        LocalDate hoy = LocalDate.of(2026, 11, 17);
        agregar("magna", 23.40, antes);
        agregar("premium", 28.00, antes);
        agregar("diesel", 26.80, antes);
        agregar("magna", 23.50, hoy);
        agregar("premium", 28.10, hoy);
        agregar("diesel", 26.90, hoy);
    }

    private static Estacion est(long id, String permiso, String nombre, String marca,
                                double lat, double lng, Double magna, Double premium, Double diesel) {
        Map<String, Double> precios = new java.util.LinkedHashMap<>();
        if (magna != null) precios.put("magna", magna);
        if (premium != null) precios.put("premium", premium);
        if (diesel != null) precios.put("diesel", diesel);
        return new Estacion(id, permiso, nombre, marca, lat, lng, "Ejemplo", null, java.util.Collections.unmodifiableMap(precios));
    }

    private synchronized MiPrecio agregar(String combustible, double precio, LocalDate fecha) {
        MiPrecio p = new MiPrecio(siguienteId.getAndIncrement(), combustible, precio, fecha);
        misPrecios.add(p);
        return p;
    }

    @Override
    public List<Estacion> estacionesEnRectangulo(double latMin, double latMax, double lngMin, double lngMax) {
        return estaciones.stream()
                .filter(e -> e.lat() >= latMin && e.lat() <= latMax && e.lng() >= lngMin && e.lng() <= lngMax)
                .toList();
    }

    @Override
    public Instant ultimaActualizacion() {
        return ACTUALIZADO;
    }

    @Override
    public synchronized List<MiPrecio> misPrecios() {
        return misPrecios.stream()
                .sorted(Comparator.comparing(MiPrecio::fecha).thenComparing(MiPrecio::id).reversed())
                .toList();
    }

    @Override
    public synchronized Optional<MiPrecio> miPrecioActual(String combustible) {
        return misPrecios().stream().filter(p -> p.combustible().equals(combustible)).findFirst();
    }

    @Override
    public MiPrecio agregarMiPrecio(String combustible, double precio) {
        return agregar(combustible, precio, LocalDate.now(Config.zona()));
    }

    @Override
    public synchronized Optional<MiPrecio> corregirMiPrecio(long id, double precio) {
        for (int i = 0; i < misPrecios.size(); i++) {
            MiPrecio p = misPrecios.get(i);
            if (p.id() == id) {
                MiPrecio nuevo = new MiPrecio(p.id(), p.combustible(), precio, p.fecha());
                misPrecios.set(i, nuevo);
                return Optional.of(nuevo);
            }
        }
        return Optional.empty();
    }

    @Override
    public String fuente() {
        return "ejemplo";
    }
}
