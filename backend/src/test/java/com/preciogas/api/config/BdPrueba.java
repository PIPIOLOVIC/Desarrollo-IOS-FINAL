package com.preciogas.api.config;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Ayudas para las pruebas *IT que usan la base real. */
public final class BdPrueba {

    private BdPrueba() {
    }

    /** Omite la prueba (no la falla) si no hay base configurada. */
    public static void requiereBase() {
        assumeTrue(Config.hayBaseDeDatos(), "Sin base configurada (db.host / PRECIOGAS_DB_HOST): se omite");
    }

    /** Primer valor numérico de la consulta (COUNT, MAX, etc.); null si viene NULL. */
    public static Double numero(String sql, Object... params) throws SQLException {
        try (Connection c = Db.abrir(); PreparedStatement ps = preparar(c, sql, params); ResultSet rs = ps.executeQuery()) {
            rs.next();
            double v = rs.getDouble(1);
            return rs.wasNull() ? null : v;
        }
    }

    public static long contar(String sql, Object... params) throws SQLException {
        return numero(sql, params).longValue();
    }

    public static int ejecutar(String sql, Object... params) throws SQLException {
        try (Connection c = Db.abrir(); PreparedStatement ps = preparar(c, sql, params)) {
            return ps.executeUpdate();
        }
    }

    private static PreparedStatement preparar(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }
}
