package com.preciogas.api.dao;

/**
 * Elige la fuente de datos una sola vez al arrancar.
 * Por ahora siempre datos de ejemplo; cuando exista JdbcDao se usará si
 * Config.hayBaseDeDatos() es true.
 */
public final class Daos {

    private static final PreciosDao INSTANCIA = new EjemploDao();

    private Daos() {
    }

    public static PreciosDao get() {
        return INSTANCIA;
    }
}
