package com.preciogas.api.model;

import java.time.LocalDate;

/** Un precio registrado de nuestra gasolinera. */
public record MiPrecio(long id, String combustible, double precio, LocalDate fecha) {
}
