package com.preciogas.api.collector;

import com.preciogas.api.config.Config;
import com.preciogas.api.dao.Daos;
import com.preciogas.api.dao.PreciosDao;
import com.preciogas.api.model.Estacion;
import com.preciogas.api.model.ResultadoActualizacion;
import com.preciogas.api.resource.ApiException;
import com.preciogas.api.service.PreciosService;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.stream.XMLStreamException;

/**
 * Colector de datos de la CNE: descarga (o lee el respaldo), une, filtra y guarda en TiDB.
 * Lo corre POST /api/admin/actualizar.
 *
 * Sin base de datos configurada solo cuenta los datos de ejemplo (para que el endpoint
 * responda según el contrato mientras tanto).
 */
public final class Colector {

    private static final Logger LOG = Logger.getLogger(Colector.class.getName());

    /** Precios fuera de este rango ($/L) son errores de captura en la CNE (hay 0.01, por ejemplo). */
    static final double PRECIO_MIN = 15.0;
    static final double PRECIO_MAX = 40.0;

    private Colector() {
    }

    /**
     * @param forzarArchivo true para no descargar y usar directo el archivo de respaldo.
     *                      Si la descarga falla, también se usa el respaldo.
     */
    public static synchronized ResultadoActualizacion ejecutar(boolean forzarArchivo) {
        if (!Config.hayBaseDeDatos()) {
            return contarEjemplo();
        }
        if (Config.miLat() == Config.MI_LAT_POR_OMISION && Config.miLng() == Config.MI_LNG_POR_OMISION) {
            throw error("Configura mi.lat y mi.lng con la ubicación real de la gasolinera antes de cargar datos");
        }

        long t0 = System.nanoTime();
        String origen = "descarga";
        byte[] places = null;
        byte[] prices = null;
        if (!forzarArchivo) {
            try {
                places = CneClient.descargar("places");
                prices = CneClient.descargar("prices");
            } catch (IOException e) {
                LOG.warning("[Colector] No se pudo descargar de la CNE (" + e.getMessage() + "); se usa el archivo de respaldo");
                prices = null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw error("Se interrumpió la descarga de la CNE");
            }
        }
        if (prices == null) {
            origen = "archivo";
            try {
                places = leerRespaldo("places");
                prices = leerRespaldo("prices");
            } catch (IOException e) {
                throw error("No se pudo leer el archivo de respaldo: " + e.getMessage());
            }
        }

        long t1 = System.nanoTime();
        Map<String, Estacion> lugares;
        List<Estacion> estaciones;
        try {
            lugares = CneParser.leerLugares(new ByteArrayInputStream(places));
            estaciones = unir(lugares, CneParser.leerPrecios(new ByteArrayInputStream(prices)),
                    Config.miLat(), Config.miLng(), Config.cneRadioKm(), Config.miPermiso());
        } catch (XMLStreamException e) {
            throw error("El archivo de la CNE (" + origen + ") no es un XML válido: " + e.getMessage());
        }

        long t2 = System.nanoTime();
        ResultadoActualizacion r;
        try {
            r = CargaCne.guardar(estaciones, LocalDate.now(Config.zona()));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "[Colector] Falló la carga en la base de datos", e);
            throw error("Falló la carga en la base de datos (SQLState " + e.getSQLState() + "): " + e.getMessage());
        } catch (IllegalStateException e) {
            throw error(e.getMessage()); // de Db.abrir(): mensaje ya sin credenciales
        }

        long t3 = System.nanoTime();
        LOG.info(String.format(Locale.ROOT,
                "[Colector] origen=%s descarga=%.1fs parseo=%.1fs carga=%.1fs estaciones=%d precios=%d descartadas=%d",
                origen, seg(t0, t1), seg(t1, t2), seg(t2, t3), r.estaciones(), r.precios(), lugares.size() - r.estaciones()));
        return r;
    }

    /**
     * Une estaciones (por place_id) con sus precios y deja solo las que:
     * tienen coordenadas, tienen al menos un precio en rango, están a radioKm o menos
     * de (lat, lng) y no son mi gasolinera (excluirPermiso, puede ser null).
     */
    static List<Estacion> unir(Map<String, Estacion> lugares, Map<String, Map<String, Double>> precios,
                               double lat, double lng, double radioKm, String excluirPermiso) {
        List<Estacion> resultado = new ArrayList<>();
        for (Map.Entry<String, Estacion> par : lugares.entrySet()) {
            Estacion e = par.getValue();
            Map<String, Double> suyos = precios.get(par.getKey());
            if (suyos == null || Double.isNaN(e.lat()) || Double.isNaN(e.lng())
                    || e.permiso().equals(excluirPermiso)
                    || PreciosService.haversineKm(lat, lng, e.lat(), e.lng()) > radioKm) {
                continue;
            }
            Map<String, Double> validos = new LinkedHashMap<>();
            suyos.forEach((c, p) -> {
                if (p >= PRECIO_MIN && p <= PRECIO_MAX) {
                    validos.put(c, p);
                }
            });
            if (!validos.isEmpty()) {
                resultado.add(new Estacion(0, e.permiso(), e.nombre(), null, e.lat(), e.lng(), null, null, validos));
            }
        }
        return resultado;
    }

    /**
     * Respaldo si la CNE no responde: la ruta de cne.archivo.places / cne.archivo.prices (config del
     * servidor, nunca de la petición) o, si no hay, la copia incluida en la WAR (resources/cne/*.xml.gz).
     */
    static byte[] leerRespaldo(String archivo) throws IOException {
        String ruta = Config.get("cne.archivo." + archivo, null);
        InputStream in = ruta != null
                ? Files.newInputStream(Path.of(ruta))
                : Colector.class.getResourceAsStream("/cne/" + archivo + ".xml.gz");
        if (in == null) {
            throw new IOException("no existe /cne/" + archivo + ".xml.gz en la WAR");
        }
        boolean gz = ruta == null || ruta.endsWith(".gz");
        try (InputStream s = gz ? new GZIPInputStream(in) : in) {
            return s.readAllBytes();
        }
    }

    private static ResultadoActualizacion contarEjemplo() {
        PreciosDao dao = Daos.get();
        List<Estacion> todas = dao.estacionesEnRectangulo(-90, 90, -180, 180);
        int precios = todas.stream().mapToInt(e -> e.precios().size()).sum();
        return new ResultadoActualizacion(todas.size(), precios);
    }

    private static ApiException error(String mensaje) {
        return new ApiException(Response.Status.INTERNAL_SERVER_ERROR, mensaje);
    }

    private static double seg(long desde, long hasta) {
        return (hasta - desde) / 1e9;
    }
}
