package com.preciogas.api.dao;

import com.preciogas.api.config.Config;

/**
 * Elige la fuente de datos una sola vez al arrancar: TiDB (JdbcDao) si
 * Config.hayBaseDeDatos() es true; si no, datos de ejemplo en memoria.
 */
public final class Daos {

    private static final PreciosDao INSTANCIA = Config.hayBaseDeDatos() ? new JdbcDao() : new EjemploDao();

    private Daos() {
    }

    public static PreciosDao get() {
        return INSTANCIA;
    }
}
