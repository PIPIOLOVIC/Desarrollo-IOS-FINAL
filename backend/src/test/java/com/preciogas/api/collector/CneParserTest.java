package com.preciogas.api.collector;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.preciogas.api.model.Estacion;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Usa un recorte del archivo real de la CNE (23 nov 2026), copiado byte a byte:
 * conserva el BOM UTF-8 y los CRLF del original.
 */
class CneParserTest {

    static byte[] fixture(String nombre) throws Exception {
        try (InputStream in = CneParserTest.class.getResourceAsStream("/cne/" + nombre + ".xml")) {
            return in.readAllBytes();
        }
    }

    static Map<String, Estacion> lugares() throws Exception {
        return CneParser.leerLugares(new ByteArrayInputStream(fixture("places")));
    }

    static Map<String, Map<String, Double>> precios() throws Exception {
        return CneParser.leerPrecios(new ByteArrayInputStream(fixture("prices")));
    }

    @Test
    void elFixtureConservaBomYCrlf() throws Exception {
        for (String f : new String[] {"places", "prices"}) {
            byte[] b = fixture(f);
            assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, Arrays.copyOf(b, 3), f + " sin BOM");
            String texto = new String(b, StandardCharsets.UTF_8);
            assertTrue(texto.contains("\r\n"), f + " sin CRLF");
            assertFalse(texto.replace("\r\n", "").contains("\n"), f + " tiene LF sueltos");
        }
    }

    @Test
    void leeEstaciones() throws Exception {
        Map<String, Estacion> l = lugares();
        assertEquals(8, l.size());
        Estacion e = l.get("2045");
        assertEquals("PL/371/EXP/ES/2015", e.permiso());
        assertEquals("MULTISERVICIOS LA PILARICA SA DE CV", e.nombre());
        assertEquals(25.7558, e.lat()); // y
        assertEquals(-108.9685, e.lng()); // x
        assertEquals(0.0, l.get("13014").lat()); // viene en 0,0 en el archivo real
    }

    @Test
    void leePreciosUniendoLugaresRepetidos() throws Exception {
        Map<String, Map<String, Double>> p = precios();
        // 11697 viene en dos nodos: diesel en uno, regular y premium en otro
        assertEquals(Map.of("diesel", 27.29, "magna", 23.69, "premium", 27.79), p.get("11697"));
        // 9453 trae diesel dos veces (27 y 21.02): se queda el primero
        assertEquals(27.0, p.get("9453").get("diesel"));
        assertEquals(0.01, p.get("10473").get("diesel")); // el parser no filtra; eso lo hace Colector.unir
        assertFalse(p.containsKey("13014"));
    }
}
