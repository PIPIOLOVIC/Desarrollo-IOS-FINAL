package com.preciogas.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Properties;

/**
 * Configuración de la API.
 *
 * Orden de búsqueda de cada valor:
 *  1. Variable de entorno (por ejemplo PRECIOGAS_DB_URL para "db.url").
 *  2. Archivo indicado en la variable PRECIOGAS_CONFIG.
 *  3. config.properties en el classpath (src/main/resources, ignorado por Git).
 *
 * Las credenciales nunca van en el repositorio; solo config.example.properties.
 */
public final class Config {

    private static final Properties PROPS = cargar();

    private Config() {
    }

    private static Properties cargar() {
        Properties p = new Properties();
        String ruta = System.getenv("PRECIOGAS_CONFIG");
        if (ruta != null && !ruta.isBlank()) {
            try (Reader r = Files.newBufferedReader(Path.of(ruta), StandardCharsets.UTF_8)) {
                p.load(r);
                return p;
            } catch (IOException e) {
                System.err.println("[PrecioGas] No se pudo leer " + ruta + ": " + e.getMessage());
            }
        }
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            System.err.println("[PrecioGas] No se pudo leer config.properties: " + e.getMessage());
        }
        return p;
    }

    public static String get(String clave, String porOmision) {
        String env = System.getenv("PRECIOGAS_" + clave.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String v = PROPS.getProperty(clave);
        return (v == null || v.isBlank()) ? porOmision : v.trim();
    }

    public static double getDouble(String clave, double porOmision) {
        String v = get(clave, null);
        if (v == null) {
            return porOmision;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return porOmision;
        }
    }

    /** true si hay datos de conexión a la base (db.url o db.host); si no, la API usa datos de ejemplo. */
    public static boolean hayBaseDeDatos() {
        return get("db.url", null) != null || get("db.host", null) != null;
    }

    /** Valores de ejemplo, no son una gasolinera real: el colector se niega a cargar con ellos. */
    public static final double MI_LAT_POR_OMISION = 20.90;
    public static final double MI_LNG_POR_OMISION = -101.30;

    /** Ubicación de nuestra gasolinera (centro para /resumen y valor por omisión de /competencia). */
    public static double miLat() {
        return getDouble("mi.lat", MI_LAT_POR_OMISION);
    }

    public static double miLng() {
        return getDouble("mi.lng", MI_LNG_POR_OMISION);
    }

    /** Permiso CNE de nuestra gasolinera: el colector no la guarda, así no entra al promedio de /resumen. */
    public static String miPermiso() {
        return get("mi.permiso", null);
    }

    /**
     * Radio en km alrededor de mi gasolinera dentro del cual el colector guarda estaciones.
     * 100 km = el radio máximo que acepta /competencia, para que nunca quede corta.
     */
    public static double cneRadioKm() {
        return getDouble("cne.radio.km", 100.0);
    }

    public static double radioPorOmision() {
        return getDouble("radio.default", 10.0);
    }

    /** Zona horaria de la gasolinera, para la fecha de los precios que registro. */
    public static ZoneId zona() {
        try {
            return ZoneId.of(get("zona", "America/Mexico_City"));
        } catch (java.time.DateTimeException e) {
            return ZoneId.of("America/Mexico_City");
        }
    }

    /** Si tiene valor, POST /admin/actualizar exige el encabezado X-Admin-Token con ese valor. */
    public static String adminToken() {
        return get("admin.token", null);
    }
}
