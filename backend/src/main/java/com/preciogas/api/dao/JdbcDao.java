package com.preciogas.api.dao;

import com.preciogas.api.config.Config;
import com.preciogas.api.config.Db;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.MiPrecio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Datos reales en TiDB (tablas de schema.sql). Daos lo usa cuando hay base configurada.
 *
 * shortcut: abre una conexión por consulta (sin pool); si las respuestas se sienten lentas
 * con TiDB Cloud, usar un pool como HikariCP.
 */
public class JdbcDao implements PreciosDao {

    /**
     * Para cada estación del rectángulo, el precio más reciente de cada combustible
     * (una estación puede no haber reportado todos los combustibles el último día).
     */
    private static final String ESTACIONES =
            "SELECT e.id, e.permiso, e.nombre, e.marca, e.latitud, e.longitud, e.municipio, p.combustible, p.precio "
            + "FROM estacion e JOIN precio p ON p.estacion_id = e.id "
            + "WHERE e.latitud BETWEEN ? AND ? AND e.longitud BETWEEN ? AND ? "
            + "AND p.fecha = (SELECT MAX(p2.fecha) FROM precio p2 "
            + "               WHERE p2.estacion_id = p.estacion_id AND p2.combustible = p.combustible) "
            + "ORDER BY e.id, p.combustible"; // ENUM: ordena magna, premium, diesel

    private static final String MIS_PRECIOS = "SELECT id, combustible, precio, fecha FROM mi_precio ";
    private static final String MAS_RECIENTE = " ORDER BY fecha DESC, id DESC";

    @Override
    public List<Estacion> estacionesEnRectangulo(double latMin, double latMax, double lngMin, double lngMax) {
        Map<Long, Estacion> porId = new LinkedHashMap<>();
        try (Connection c = Db.abrir(); PreparedStatement ps = c.prepareStatement(ESTACIONES)) {
            ps.setDouble(1, latMin);
            ps.setDouble(2, latMax);
            ps.setDouble(3, lngMin);
            ps.setDouble(4, lngMax);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    Estacion e = porId.get(id);
                    if (e == null) {
                        e = new Estacion(id, rs.getString("permiso"), rs.getString("nombre"), rs.getString("marca"),
                                rs.getDouble("latitud"), rs.getDouble("longitud"), rs.getString("municipio"),
                                null, new LinkedHashMap<>());
                        porId.put(id, e);
                    }
                    e.precios().put(rs.getString("combustible"), rs.getBigDecimal("precio").doubleValue());
                }
            }
        } catch (SQLException e) {
            throw falla(e);
        }
        return new ArrayList<>(porId.values());
    }

    @Override
    public Instant ultimaActualizacion() {
        // UNIX_TIMESTAMP interpreta la fecha en la zona de la sesión, la misma con la que NOW() la escribió.
        try (Connection c = Db.abrir(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT UNIX_TIMESTAMP(MAX(actualizado)) FROM estacion")) {
            rs.next();
            long segundos = rs.getLong(1);
            return rs.wasNull() ? null : Instant.ofEpochSecond(segundos);
        } catch (SQLException e) {
            throw falla(e);
        }
    }

    @Override
    public List<MiPrecio> misPrecios() {
        try (Connection c = Db.abrir(); PreparedStatement ps = c.prepareStatement(MIS_PRECIOS + MAS_RECIENTE)) {
            return leer(ps);
        } catch (SQLException e) {
            throw falla(e);
        }
    }

    @Override
    public Optional<MiPrecio> miPrecioActual(String combustible) {
        try (Connection c = Db.abrir();
             PreparedStatement ps = c.prepareStatement(MIS_PRECIOS + "WHERE combustible = ?" + MAS_RECIENTE + " LIMIT 1")) {
            ps.setString(1, combustible);
            return leer(ps).stream().findFirst();
        } catch (SQLException e) {
            throw falla(e);
        }
    }

    @Override
    public MiPrecio agregarMiPrecio(String combustible, double precio) {
        BigDecimal valor = dosDecimales(precio);
        LocalDate hoy = LocalDate.now(Config.zona());
        try (Connection c = Db.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO mi_precio (combustible, precio, fecha) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, combustible);
            ps.setBigDecimal(2, valor);
            ps.setDate(3, Date.valueOf(hoy));
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return new MiPrecio(llaves.getLong(1), combustible, valor.doubleValue(), hoy);
            }
        } catch (SQLException e) {
            throw falla(e);
        }
    }

    @Override
    public Optional<MiPrecio> corregirMiPrecio(long id, double precio) {
        try (Connection c = Db.abrir();
             PreparedStatement upd = c.prepareStatement("UPDATE mi_precio SET precio = ? WHERE id = ?");
             PreparedStatement sel = c.prepareStatement(MIS_PRECIOS + "WHERE id = ?")) {
            upd.setBigDecimal(1, dosDecimales(precio));
            upd.setLong(2, id);
            upd.executeUpdate();
            // Se relee en lugar de usar el conteo: MySQL cuenta 0 filas si el precio no cambió.
            sel.setLong(1, id);
            return leer(sel).stream().findFirst();
        } catch (SQLException e) {
            throw falla(e);
        }
    }

    @Override
    public String fuente() {
        return "bd";
    }

    private static List<MiPrecio> leer(PreparedStatement ps) throws SQLException {
        List<MiPrecio> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new MiPrecio(rs.getLong("id"), rs.getString("combustible"),
                        rs.getBigDecimal("precio").doubleValue(), rs.getDate("fecha").toLocalDate()));
            }
        }
        return lista;
    }

    private static BigDecimal dosDecimales(double precio) {
        return BigDecimal.valueOf(precio).setScale(2, RoundingMode.HALF_UP);
    }

    /** ErrorMapper la registra completa y al cliente le responde 500 genérico. */
    private static IllegalStateException falla(SQLException e) {
        return new IllegalStateException("Error de base de datos (SQLState " + e.getSQLState() + "): " + e.getMessage(), e);
    }
}
