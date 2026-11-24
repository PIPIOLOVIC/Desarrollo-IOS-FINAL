package com.preciogas.api.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.preciogas.api.model.Estacion;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ColectorTest {

    private static Map<String, Estacion> porPermiso(List<Estacion> l) {
        return l.stream().collect(Collectors.toMap(Estacion::permiso, e -> e));
    }

    @Test
    void filtraPorRadioYExcluyeMiGasolinera() throws Exception {
        // Centro de La Piedad, 100 km, excluyendo AUTOSERVICIO EL FUERTE
        Map<String, Estacion> r = porPermiso(Colector.unir(CneParserTest.lugares(), CneParserTest.precios(),
                20.34, -102.03, 100, "PL/2810/EXP/ES/2015"));
        assertEquals(java.util.Set.of("PL/1395/EXP/ES/2015", "PL/8668/EXP/ES/2015"), r.keySet());
        assertEquals(Map.of("magna", 23.87, "premium", 28.99), r.get("PL/8668/EXP/ES/2015").precios());
    }

    @Test
    void elRespaldoIncluidoEnLaWarSeLee() throws Exception {
        assertEquals(15050, CneParser.leerLugares(new java.io.ByteArrayInputStream(Colector.leerRespaldo("places"))).size());
        assertEquals(13843, CneParser.leerPrecios(new java.io.ByteArrayInputStream(Colector.leerRespaldo("prices"))).size());
    }

    @Test
    void descartaPreciosAbsurdosYEstacionesSinPrecio() throws Exception {
        // Radio enorme: todo México. 13014 (0,0 y sin precio) no entra; a 10473 se le quita el diesel de 0.01.
        List<Estacion> todas = Colector.unir(CneParserTest.lugares(), CneParserTest.precios(), 20.34, -102.03, 5000, null);
        Map<String, Estacion> r = porPermiso(todas);
        assertEquals(7, todas.size());
        Estacion e10473 = todas.stream().filter(e -> e.lat() == 18.15451).findFirst().orElseThrow();
        assertEquals(Map.of("magna", 22.89, "premium", 26.69), e10473.precios());
        assertEquals(true, r.containsKey("PL/2810/EXP/ES/2015")); // sin excluir, mi gasolinera sí está
    }
}
