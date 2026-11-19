package com.preciogas.api.model;

/** Respuesta de POST /admin/actualizar: cuántas estaciones y precios se cargaron. */
public record ResultadoActualizacion(int estaciones, int precios) {
}
