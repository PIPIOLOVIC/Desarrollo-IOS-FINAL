package com.preciogas.api.model;

/**
 * Respuesta de GET /resumen. Los campos numéricos son null si no hay datos
 * (por ejemplo, ninguna estación en el radio o aún no registro mi precio).
 */
public record Resumen(
        String combustible,
        Double promedio,
        Double minimo,
        Double maximo,
        Double miPrecio,
        Double diferencia,
        int estaciones) {
}
