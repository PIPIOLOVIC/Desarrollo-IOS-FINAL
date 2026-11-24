package com.preciogas.api.config;

import static com.preciogas.api.config.BdPrueba.contar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.preciogas.api.service.PreciosService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Revisa la base real: conexión, esquema y que los datos cargados de la CNE tengan sentido. */
class BaseDeDatosIT {

    @BeforeAll
    static void soloConBase() {
        BdPrueba.requiereBase();
    }

    @Test
    void conecta() throws Exception {
        try (Connection c = Db.abrir()) {
            assertTrue(c.isValid(10));
        }
    }

    @Test
    void tablasTienenLasColumnasDelEsquema() throws Exception {
        Map<String, Set<String>> columnas = new HashMap<>();
        try (Connection c = Db.abrir(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT LOWER(table_name), LOWER(column_name) FROM information_schema.columns "
                     + "WHERE table_schema = DATABASE()")) {
            while (rs.next()) {
                columnas.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getString(2));
            }
        }
        assertTrue(columnas.getOrDefault("estacion", Set.of()).containsAll(
                Set.of("id", "permiso", "nombre", "marca", "latitud", "longitud", "municipio", "actualizado")), "estacion: " + columnas.get("estacion"));
        assertTrue(columnas.getOrDefault("precio", Set.of()).containsAll(
                Set.of("id", "estacion_id", "combustible", "precio", "fecha")), "precio: " + columnas.get("precio"));
        assertTrue(columnas.getOrDefault("mi_precio", Set.of()).containsAll(
                Set.of("id", "combustible", "precio", "fecha")), "mi_precio: " + columnas.get("mi_precio"));
    }

    @Test
    void hayDatosDeLaCneCargados() throws Exception {
        assertTrue(contar("SELECT COUNT(*) FROM estacion") > 0, "estacion está vacía: corre POST /api/admin/actualizar");
        assertTrue(contar("SELECT COUNT(*) FROM precio") > 0, "precio está vacía: corre POST /api/admin/actualizar");
    }

    @Test
    void losPreciosMasRecientesSonDeEstaSemana() throws Exception {
        LocalDate ultima;
        try (Connection c = Db.abrir(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT MAX(fecha) FROM precio")) {
            rs.next();
            assertNotNull(rs.getDate(1), "No hay precios");
            ultima = rs.getDate(1).toLocalDate();
        }
        long dias = ChronoUnit.DAYS.between(ultima, LocalDate.now(Config.zona()));
        assertTrue(dias <= 7, "La última carga de la CNE es del " + ultima + " (hace " + dias + " días): corre el colector");
    }

    @Test
    void preciosEnRangoYConCombustiblesDelContrato() throws Exception {
        assertEquals(0, contar("SELECT COUNT(*) FROM precio WHERE precio < 15 OR precio > 40"), "Hay precios fuera de 15-40 $/L");
        assertEquals(0, contar("SELECT COUNT(*) FROM precio WHERE combustible NOT IN ('magna', 'premium', 'diesel')"));
    }

    @Test
    void noHayPreciosSinEstacionNiDuplicados() throws Exception {
        assertEquals(0, contar("SELECT COUNT(*) FROM precio p LEFT JOIN estacion e ON e.id = p.estacion_id WHERE e.id IS NULL"));
        assertEquals(0, contar("SELECT COUNT(*) FROM (SELECT 1 FROM precio GROUP BY estacion_id, combustible, fecha HAVING COUNT(*) > 1) d"));
        assertEquals(0, contar("SELECT COUNT(*) FROM (SELECT 1 FROM estacion GROUP BY permiso HAVING COUNT(*) > 1) d"));
    }

    @Test
    void miGasolineraNoEstaComoCompetencia() throws Exception {
        String permiso = Config.miPermiso();
        if (permiso != null) {
            assertEquals(0, contar("SELECT COUNT(*) FROM estacion WHERE permiso = ?", permiso));
        }
    }

    @Test
    void lasEstacionesEstanDentroDelRadioDeCarga() throws Exception {
        List<String> fuera = new ArrayList<>();
        try (Connection c = Db.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT permiso, latitud, longitud FROM estacion");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double km = PreciosService.haversineKm(Config.miLat(), Config.miLng(), rs.getDouble(2), rs.getDouble(3));
                if (km > Config.cneRadioKm() + 0.5) {
                    fuera.add(rs.getString(1) + " (" + Math.round(km) + " km)");
                }
            }
        }
        assertTrue(fuera.isEmpty(), "Estaciones más allá de cne.radio.km de mi.lat/mi.lng: " + fuera.subList(0, Math.min(5, fuera.size())));
    }
}
