package com.preciogas.api.dao;

import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.MiPrecio;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a los datos. Hoy existe EjemploDao (en memoria); cuando la base TiDB
 * esté lista se agrega JdbcDao con la misma interfaz y los endpoints no cambian.
 */
public interface PreciosDao {

    /** Estaciones dentro del rectángulo de coordenadas, con sus precios más recientes. */
    List<Estacion> estacionesEnRectangulo(double latMin, double latMax, double lngMin, double lngMax);

    /** Fecha de la última carga de datos de la CNE. */
    Instant ultimaActualizacion();

    /** Todos mis precios, del más reciente al más antiguo (actuales e historial). */
    List<MiPrecio> misPrecios();

    /** Mi precio más reciente para un combustible. */
    Optional<MiPrecio> miPrecioActual(String combustible);

    MiPrecio agregarMiPrecio(String combustible, double precio);

    Optional<MiPrecio> corregirMiPrecio(long id, double precio);

    /** Nombre de la fuente de datos, para /salud. */
    String fuente();
}
