package com.preciogas.api.collector;

import com.preciogas.api.config.Db;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.ResultadoActualizacion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda en TiDB las estaciones de la CNE y sus precios del día, en una sola transacción.
 * - estacion: inserta o actualiza por permiso (único) y marca actualizado = NOW().
 * - precio: una fila por estación, combustible y fecha (historial); si se corre dos veces
 *   el mismo día, se actualiza el precio de ese día en lugar de duplicarlo.
 */
final class CargaCne {

    private static final String UPSERT_ESTACION =
            "INSERT INTO estacion (permiso, nombre, latitud, longitud) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), latitud = VALUES(latitud), "
            // Explícito: ON UPDATE CURRENT_TIMESTAMP no se dispara si la fila no cambia.
            + "longitud = VALUES(longitud), actualizado = NOW()";

    private static final String UPSERT_PRECIO =
            "INSERT INTO precio (estacion_id, combustible, precio, fecha) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE precio = VALUES(precio)";

    private CargaCne() {
    }

    static ResultadoActualizacion guardar(List<Estacion> estaciones, LocalDate fecha) throws SQLException {
        try (Connection c = Db.abrir()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(UPSERT_ESTACION)) {
                    for (Estacion e : estaciones) {
                        ps.setString(1, e.permiso());
                        ps.setString(2, e.nombre());
                        ps.setBigDecimal(3, BigDecimal.valueOf(e.lat()).setScale(6, RoundingMode.HALF_UP));
                        ps.setBigDecimal(4, BigDecimal.valueOf(e.lng()).setScale(6, RoundingMode.HALF_UP));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                Map<String, Long> ids = new HashMap<>();
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT id, permiso FROM estacion")) {
                    while (rs.next()) {
                        ids.put(rs.getString(2), rs.getLong(1));
                    }
                }

                int precios = 0;
                try (PreparedStatement ps = c.prepareStatement(UPSERT_PRECIO)) {
                    for (Estacion e : estaciones) {
                        for (Map.Entry<String, Double> p : e.precios().entrySet()) {
                            ps.setLong(1, ids.get(e.permiso()));
                            ps.setString(2, p.getKey());
                            ps.setBigDecimal(3, BigDecimal.valueOf(p.getValue()).setScale(2, RoundingMode.HALF_UP));
                            ps.setDate(4, Date.valueOf(fecha));
                            ps.addBatch();
                            precios++;
                        }
                    }
                    ps.executeBatch();
                }
                c.commit();
                return new ResultadoActualizacion(estaciones.size(), precios);
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
