package com.preciogas.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.preciogas.api.resource.ApiException;
import org.junit.jupiter.api.Test;

class CombustibleTest {

    @Test
    void normalizaMayusculasYEspacios() {
        assertEquals("diesel", Combustible.validar("  DIESEL "));
    }

    @Test
    void rechazaVacioYDesconocidoCon400() {
        assertEquals(400, assertThrows(ApiException.class, () -> Combustible.validar(null)).getResponse().getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> Combustible.validar("gas")).getResponse().getStatus());
    }
}
