package com.preciogas.api.collector;

import com.preciogas.api.dao.Daos;
import com.preciogas.api.dao.PreciosDao;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.ResultadoActualizacion;
import java.util.List;

/**
 * Colector de datos de la CNE.
 *
 * PENDIENTE (persona A): descargar con HttpClient (CneClient), leer el archivo
 * (CneParser), unir estaciones y precios por permiso, filtrar por municipio y
 * guardar con INSERT ... ON DUPLICATE KEY UPDATE. Mientras tanto solo cuenta
 * los datos de ejemplo para que el endpoint ya responda según el contrato.
 */
public final class Colector {

    private Colector() {
    }

    public static synchronized ResultadoActualizacion ejecutar() {
        PreciosDao dao = Daos.get();
        List<Estacion> todas = dao.estacionesEnRectangulo(-90, 90, -180, 180);
        int precios = todas.stream().mapToInt(e -> e.precios().size()).sum();
        return new ResultadoActualizacion(todas.size(), precios);
    }
}
