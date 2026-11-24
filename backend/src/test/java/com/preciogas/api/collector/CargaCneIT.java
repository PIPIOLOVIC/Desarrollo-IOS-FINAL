package com.preciogas.api.collector;

import static com.preciogas.api.config.BdPrueba.contar;
import static com.preciogas.api.config.BdPrueba.numero;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.preciogas.api.config.BdPrueba;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.ResultadoActualizacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CargaCne contra la base real, con una estación de prueba (permiso PRUEBA/JUNIT/...) en el mar,
 * lejos de cualquier radio, y fechas del año 2000. Se borra antes y después de cada prueba.
 */
class CargaCneIT {

    private static final String PERMISO = "PRUEBA/JUNIT/0001";
    private static final LocalDate DIA_1 = LocalDate.of(2000, 1, 1);
    private static final LocalDate DIA_2 = LocalDate.of(2000, 1, 2);

    @BeforeAll
    static void soloConBase() {
        BdPrueba.requiereBase();
    }

    @BeforeEach
    void antes() throws Exception {
        limpiar();
    }

    @AfterEach
    void despues() throws Exception {
        limpiar();
    }

    private static void limpiar() throws Exception {
        BdPrueba.ejecutar("DELETE FROM precio WHERE estacion_id IN (SELECT id FROM estacion WHERE permiso LIKE 'PRUEBA/JUNIT/%')");
        BdPrueba.ejecutar("DELETE FROM estacion WHERE permiso LIKE 'PRUEBA/JUNIT/%'");
    }

    private static Estacion estacion(String nombre, Map<String, Double> precios) {
        return new Estacion(0, PERMISO, nombre, null, -0.45, -0.45, null, null, precios);
    }

    private static long preciosDePrueba() throws Exception {
        return contar("SELECT COUNT(*) FROM precio p JOIN estacion e ON e.id = p.estacion_id WHERE e.permiso = ?", PERMISO);
    }

    private static Double precio(String combustible, LocalDate fecha) throws Exception {
        return numero("SELECT p.precio FROM precio p JOIN estacion e ON e.id = p.estacion_id "
                + "WHERE e.permiso = ? AND p.combustible = ? AND p.fecha = ?", PERMISO, combustible, fecha);
    }

    @Test
    void guardaEstacionYPrecios() throws Exception {
        ResultadoActualizacion r = CargaCne.guardar(
                List.of(estacion("ESTACION DE PRUEBA", Map.of("magna", 20.0, "premium", 25.555))), DIA_1);

        assertEquals(1, r.estaciones());
        assertEquals(2, r.precios());
        assertEquals(1, contar("SELECT COUNT(*) FROM estacion WHERE permiso = ?", PERMISO));
        assertEquals(2, preciosDePrueba());
        assertEquals(25.56, precio("premium", DIA_1), 0.0001, "El precio se guarda con 2 decimales");
        try (Connection c = com.preciogas.api.config.Db.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT nombre, latitud, longitud FROM estacion WHERE permiso = ?")) {
            ps.setString(1, PERMISO);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("ESTACION DE PRUEBA", rs.getString(1));
                assertEquals(-0.45, rs.getDouble(2), 0.000001);
                assertEquals(-0.45, rs.getDouble(3), 0.000001);
            }
        }
    }

    @Test
    void cargarDosVecesElMismoDiaNoDuplicaYActualiza() throws Exception {
        CargaCne.guardar(List.of(estacion("NOMBRE VIEJO", Map.of("magna", 20.0, "diesel", 22.0))), DIA_1);
        CargaCne.guardar(List.of(estacion("NOMBRE NUEVO", Map.of("magna", 21.0, "diesel", 22.0))), DIA_1);

        assertEquals(1, contar("SELECT COUNT(*) FROM estacion WHERE permiso = ?", PERMISO));
        assertEquals(1, contar("SELECT COUNT(*) FROM estacion WHERE permiso = ? AND nombre = 'NOMBRE NUEVO'", PERMISO));
        assertEquals(2, preciosDePrueba());
        assertEquals(21.0, precio("magna", DIA_1), 0.0001);
    }

    @Test
    void otroDiaAgregaHistorial() throws Exception {
        CargaCne.guardar(List.of(estacion("ESTACION DE PRUEBA", Map.of("magna", 20.0))), DIA_1);
        CargaCne.guardar(List.of(estacion("ESTACION DE PRUEBA", Map.of("magna", 20.5))), DIA_2);

        assertEquals(2, preciosDePrueba());
        assertEquals(20.0, precio("magna", DIA_1), 0.0001);
        assertEquals(20.5, precio("magna", DIA_2), 0.0001);
    }
}
