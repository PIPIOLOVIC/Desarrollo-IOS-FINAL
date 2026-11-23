package com.preciogas.api.service;

import com.preciogas.api.config.Config;
import com.preciogas.api.dao.PreciosDao;
import com.preciogas.api.model.Competencia;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.MiPrecio;
import com.preciogas.api.model.Resumen;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/** Lógica de /competencia y /resumen. */
public class PreciosService {

    private static final double RADIO_TIERRA_KM = 6371.0;

    private final PreciosDao dao;

    public PreciosService(PreciosDao dao) {
        this.dao = dao;
    }

    /**
     * Estaciones a menos de radioKm del punto, ordenadas por distancia.
     * Si se indica combustible, solo las que lo venden.
     * Primero filtra con un rectángulo (rápido en la base) y luego con Haversine (exacto).
     */
    public Competencia competencia(double lat, double lng, double radioKm, String combustible) {
        double dLat = Math.toDegrees(radioKm / RADIO_TIERRA_KM);
        double dLng = Math.toDegrees(radioKm / (RADIO_TIERRA_KM * Math.cos(Math.toRadians(lat))));

        List<Estacion> cercanas = dao.estacionesEnRectangulo(lat - dLat, lat + dLat, lng - dLng, lng + dLng)
                .stream()
                .filter(e -> combustible == null || e.precios().containsKey(combustible))
                .map(e -> e.conDistancia(redondear(haversineKm(lat, lng, e.lat(), e.lng()))))
                .filter(e -> e.distanciaKm() <= radioKm)
                .sorted(Comparator.comparingDouble(Estacion::distanciaKm))
                .toList();

        return new Competencia(dao.ultimaActualizacion(), cercanas);
    }

    /**
     * Promedio, mínimo y máximo de la zona (radio por omisión alrededor de mi gasolinera)
     * y diferencia de mi precio contra el promedio (negativo = estoy más barato).
     * Mi propia estación no está en la tabla de competencia, así que no entra al promedio.
     */
    public Resumen resumen(String combustible) {
        List<Estacion> zona = competencia(Config.miLat(), Config.miLng(), Config.radioPorOmision(), combustible)
                .estaciones();
        DoubleSummaryStatistics stats = zona.stream()
                .mapToDouble(e -> e.precios().get(combustible))
                .summaryStatistics();

        Double promedio = stats.getCount() == 0 ? null : redondear(stats.getAverage());
        Double minimo = stats.getCount() == 0 ? null : stats.getMin();
        Double maximo = stats.getCount() == 0 ? null : stats.getMax();
        Double miPrecio = dao.miPrecioActual(combustible).map(MiPrecio::precio).orElse(null);
        Double diferencia = (miPrecio == null || promedio == null) ? null : redondear(miPrecio - promedio);

        return new Resumen(combustible, promedio, minimo, maximo, miPrecio, diferencia, (int) stats.getCount());
    }

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(a));
    }

    static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
