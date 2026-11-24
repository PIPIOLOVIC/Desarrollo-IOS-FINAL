package com.preciogas.api.collector;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Descarga los XML de precios y estaciones que publica la CNE (antes CRE). */
public final class CneClient {

    static final String URL_BASE = "https://publicacionexterna.azurewebsites.net/publicaciones/";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private CneClient() {
    }

    /**
     * Descarga "places" o "prices" completo (unos 3 MB; tarda ~12 s).
     * El servidor responde Content-Type text/html aunque el cuerpo es XML, así que no se revisa.
     */
    public static byte[] descargar(String archivo) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL_BASE + archivo))
                .timeout(Duration.ofSeconds(120))
                .GET()
                .build();
        HttpResponse<byte[]> r = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) {
            throw new IOException("La CNE respondió HTTP " + r.statusCode() + " al descargar " + archivo);
        }
        return r.body();
    }
}
