package com.preciogas.api.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ColectorProgramadoTest {

    private static final ZoneId MX = ZoneId.of("America/Mexico_City");

    private static ZonedDateTime a(int h, int m) {
        return ZonedDateTime.of(2026, 11, 23, h, m, 0, 0, MX);
    }

    @Test
    void siLaHoraNoHaPasadoCorreHoy() {
        assertEquals(3600, ColectorProgramado.segundosHasta(LocalTime.of(7, 0), a(6, 0)));
    }

    @Test
    void siYaPasoCorreManana() {
        assertEquals(23 * 3600, ColectorProgramado.segundosHasta(LocalTime.of(7, 0), a(8, 0)));
    }

    @Test
    void justoALaHoraEsperaUnDia() {
        assertEquals(24 * 3600, ColectorProgramado.segundosHasta(LocalTime.of(7, 0), a(7, 0)));
    }
}
