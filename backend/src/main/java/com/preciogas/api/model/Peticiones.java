package com.preciogas.api.model;

/** Cuerpos JSON que recibe la API. */
public final class Peticiones {

    private Peticiones() {
    }

    /** POST /mis-precios: { "combustible": "magna", "precio": 23.50 } */
    public record NuevoPrecio(String combustible, Double precio) {
    }

    /** PUT /mis-precios/{id}: { "precio": 23.60 } */
    public record CorreccionPrecio(Double precio) {
    }
}
