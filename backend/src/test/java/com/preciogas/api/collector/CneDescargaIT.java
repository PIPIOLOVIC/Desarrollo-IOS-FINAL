package com.preciogas.api.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.preciogas.api.config.Config;
import com.preciogas.api.model.Estacion;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Descarga los archivos reales de la CNE (unos 25 s) y revisa que el formato siga siendo el que
 * espera el parser. No usa la base. Si falla, la CNE cambió algo y el colector usaría el respaldo.
 */
class CneDescargaIT {

    @Test
    void laCneSigueSirviendoElFormatoEsperado() throws Exception {
        Map<String, Estacion> lugares = CneParser.leerLugares(new ByteArrayInputStream(CneClient.descargar("places")));
        Map<String, Map<String, Double>> precios = CneParser.leerPrecios(new ByteArrayInputStream(CneClient.descargar("prices")));

        assertTrue(lugares.size() > 10_000, "Muy pocas estaciones: " + lugares.size());
        assertTrue(precios.size() > 10_000, "Muy pocos lugares con precio: " + precios.size());

        Set<String> combustibles = new HashSet<>();
        precios.values().forEach(p -> combustibles.addAll(p.keySet()));
        assertEquals(Set.of("magna", "premium", "diesel"), combustibles);

        long conPrecio = precios.keySet().stream().filter(lugares::containsKey).count();
        assertTrue(conPrecio > precios.size() * 0.95, "Los place_id de precios ya no coinciden con los de estaciones");

        String permiso = Config.miPermiso();
        if (permiso != null) {
            assertTrue(lugares.values().stream().anyMatch(e -> permiso.equals(e.permiso())),
                    "Mi gasolinera (mi.permiso) ya no aparece en la CNE");
        }
    }
}
