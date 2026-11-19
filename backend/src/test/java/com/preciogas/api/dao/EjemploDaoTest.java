package com.preciogas.api.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.preciogas.api.model.MiPrecio;
import java.util.List;
import org.junit.jupiter.api.Test;

class EjemploDaoTest {

    @Test
    void misPreciosVaDelMasRecienteAlMasAntiguo() {
        List<MiPrecio> lista = new EjemploDao().misPrecios();
        for (int i = 1; i < lista.size(); i++) {
            assertTrue(!lista.get(i).fecha().isAfter(lista.get(i - 1).fecha()));
        }
    }

    @Test
    void elPrecioNuevoPasaASerElActual() {
        EjemploDao dao = new EjemploDao();
        MiPrecio nuevo = dao.agregarMiPrecio("magna", 24.10);
        assertEquals(nuevo, dao.miPrecioActual("magna").orElseThrow());
    }

    @Test
    void corregirCambiaSoloElPrecio() {
        EjemploDao dao = new EjemploDao();
        MiPrecio antes = dao.misPrecios().get(0);
        MiPrecio despues = dao.corregirMiPrecio(antes.id(), 30.00).orElseThrow();
        assertEquals(antes.id(), despues.id());
        assertEquals(antes.combustible(), despues.combustible());
        assertEquals(antes.fecha(), despues.fecha());
        assertEquals(30.00, despues.precio());
        assertTrue(dao.corregirMiPrecio(9999, 1).isEmpty());
    }
}
