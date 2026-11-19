package com.preciogas.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;

/**
 * Estación de la competencia con sus precios actuales por combustible.
 * "precios" usa las claves magna, premium y diesel.
 */
@JsonPropertyOrder({"id", "permiso", "nombre", "marca", "lat", "lng", "distanciaKm", "precios"})
public record Estacion(
        long id,
        String permiso,
        String nombre,
        String marca,
        double lat,
        double lng,
        @JsonIgnore String municipio,
        Double distanciaKm,
        Map<String, Double> precios) {

    public Estacion conDistancia(double km) {
        return new Estacion(id, permiso, nombre, marca, lat, lng, municipio, km, precios);
    }
}
