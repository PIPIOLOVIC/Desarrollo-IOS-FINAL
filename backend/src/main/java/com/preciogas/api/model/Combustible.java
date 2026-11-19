package com.preciogas.api.model;

import com.preciogas.api.resource.ApiException;
import java.util.List;
import java.util.Locale;

/** Combustibles válidos, siempre en minúsculas como dice el contrato. */
public final class Combustible {

    public static final String MAGNA = "magna";
    public static final String PREMIUM = "premium";
    public static final String DIESEL = "diesel";
    public static final List<String> TODOS = List.of(MAGNA, PREMIUM, DIESEL);

    private Combustible() {
    }

    /** Normaliza y valida; lanza 400 si no es magna, premium o diesel. */
    public static String validar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw ApiException.badRequest("Falta el combustible (magna, premium o diesel)");
        }
        String c = valor.trim().toLowerCase(Locale.ROOT);
        if (!TODOS.contains(c)) {
            throw ApiException.badRequest("Combustible no válido: " + valor + " (usa magna, premium o diesel)");
        }
        return c;
    }
}
