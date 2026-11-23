package com.preciogas.api.collector;

import com.preciogas.api.dao.Daos;
import com.preciogas.api.dao.PreciosDao;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.ResultadoActualizacion;
import com.preciogas.api.service.PreciosService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Colector de datos de la CNE.
 *
 * PENDIENTE (persona A): descargar con HttpClient (CneClient), leer el archivo
 * (CneParser), unir estaciones y precios por permiso, filtrar por municipio y
 * guardar con INSERT ... ON DUPLICATE KEY UPDATE. Mientras tanto solo cuenta
 * los datos de ejemplo para que el endpoint ya responda según el contrato.
 */
public final class Colector {

    /** Precios fuera de este rango ($/L) son errores de captura en la CNE (hay 0.01, por ejemplo). */
    static final double PRECIO_MIN = 15.0;
    static final double PRECIO_MAX = 40.0;

    private Colector() {
    }

    public static synchronized ResultadoActualizacion ejecutar() {
        PreciosDao dao = Daos.get();
        List<Estacion> todas = dao.estacionesEnRectangulo(-90, 90, -180, 180);
        int precios = todas.stream().mapToInt(e -> e.precios().size()).sum();
        return new ResultadoActualizacion(todas.size(), precios);
    }

    /**
     * Une estaciones (por place_id) con sus precios y deja solo las que:
     * tienen coordenadas, tienen al menos un precio en rango, están a radioKm o menos
     * de (lat, lng) y no son mi gasolinera (excluirPermiso, puede ser null).
     */
    static List<Estacion> unir(Map<String, Estacion> lugares, Map<String, Map<String, Double>> precios,
                               double lat, double lng, double radioKm, String excluirPermiso) {
        List<Estacion> resultado = new ArrayList<>();
        for (Map.Entry<String, Estacion> par : lugares.entrySet()) {
            Estacion e = par.getValue();
            Map<String, Double> suyos = precios.get(par.getKey());
            if (suyos == null || Double.isNaN(e.lat()) || Double.isNaN(e.lng())
                    || e.permiso().equals(excluirPermiso)
                    || PreciosService.haversineKm(lat, lng, e.lat(), e.lng()) > radioKm) {
                continue;
            }
            Map<String, Double> validos = new LinkedHashMap<>();
            suyos.forEach((c, p) -> {
                if (p >= PRECIO_MIN && p <= PRECIO_MAX) {
                    validos.put(c, p);
                }
            });
            if (!validos.isEmpty()) {
                resultado.add(new Estacion(0, e.permiso(), e.nombre(), null, e.lat(), e.lng(), null, null, validos));
            }
        }
        return resultado;
    }
}
