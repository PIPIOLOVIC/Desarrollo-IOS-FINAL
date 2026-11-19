package com.preciogas.api.model;

import java.time.Instant;
import java.util.List;

/** Respuesta de GET /competencia. */
public record Competencia(Instant actualizado, List<Estacion> estaciones) {
}
