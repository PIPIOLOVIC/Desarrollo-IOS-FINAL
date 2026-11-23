package com.preciogas.api.collector;

import com.preciogas.api.model.Combustible;
import com.preciogas.api.model.Estacion;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Lee los dos XML de la CNE (publicacionexterna.azurewebsites.net/publicaciones/places y /prices)
 * con StAX, sin cargar el árbol completo en memoria. Formato real (verificado el 23 nov 2026):
 *
 * places: <places><place place_id="2039"><name>..</name><cre_id>PL/658/EXP/ES/2015</cre_id>
 *         <location><x>-116.9214</x><y>32.47641</y></location></place>...   (x = longitud, y = latitud)
 * prices: <places><place place_id="11702"><gas_price type="regular">24.5</gas_price>...</place>...
 *
 * Los dos archivos se unen por place_id. Un mismo place_id puede repetirse en prices.
 */
public final class CneParser {

    private static final Logger LOG = Logger.getLogger(CneParser.class.getName());

    /** Nombre del combustible en la CNE -> nombre del contrato de la API. */
    private static final Map<String, String> COMBUSTIBLES = Map.of(
            "regular", Combustible.MAGNA,
            "premium", Combustible.PREMIUM,
            "diesel", Combustible.DIESEL);

    private CneParser() {
    }

    /**
     * places -> mapa place_id -> Estacion (id 0, sin marca ni municipio, sin precios).
     * lat/lng quedan en NaN si faltan o no son números.
     */
    public static Map<String, Estacion> leerLugares(InputStream in) throws XMLStreamException {
        Map<String, Estacion> lugares = new HashMap<>();
        XMLStreamReader r = lector(in);
        try {
            String id = null, nombre = null, permiso = null, x = null, y = null;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    switch (r.getLocalName()) {
                        case "place" -> {
                            id = r.getAttributeValue(null, "place_id");
                            nombre = permiso = x = y = null;
                        }
                        case "name" -> nombre = r.getElementText().trim();
                        case "cre_id" -> permiso = r.getElementText().trim();
                        case "x" -> x = r.getElementText();
                        case "y" -> y = r.getElementText();
                        default -> {
                        }
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("place")) {
                    if (id != null && permiso != null && !permiso.isEmpty() && nombre != null) {
                        lugares.put(id, new Estacion(0, permiso, nombre, null, numero(y), numero(x), null, null, Map.of()));
                    }
                }
            }
        } finally {
            r.close();
        }
        return lugares;
    }

    /**
     * prices -> mapa place_id -> (combustible del contrato -> precio).
     * Si un lugar trae dos veces el mismo combustible con valores distintos, se queda el primero.
     */
    public static Map<String, Map<String, Double>> leerPrecios(InputStream in) throws XMLStreamException {
        Map<String, Map<String, Double>> precios = new HashMap<>();
        XMLStreamReader r = lector(in);
        int conflictos = 0;
        try {
            String id = null;
            while (r.hasNext()) {
                if (r.next() != XMLStreamConstants.START_ELEMENT) {
                    continue;
                }
                if (r.getLocalName().equals("place")) {
                    id = r.getAttributeValue(null, "place_id");
                } else if (r.getLocalName().equals("gas_price") && id != null) {
                    String combustible = COMBUSTIBLES.get(r.getAttributeValue(null, "type"));
                    double precio = numero(r.getElementText());
                    if (combustible == null || Double.isNaN(precio)) {
                        continue;
                    }
                    Double antes = precios.computeIfAbsent(id, k -> new LinkedHashMap<>()).putIfAbsent(combustible, precio);
                    if (antes != null && antes != precio) {
                        conflictos++;
                    }
                }
            }
        } finally {
            r.close();
        }
        if (conflictos > 0) {
            LOG.info("[CNE] " + conflictos + " precios repetidos con otro valor para el mismo lugar y combustible; se usó el primero");
        }
        return precios;
    }

    private static XMLStreamReader lector(InputStream in) throws XMLStreamException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        // El archivo viene de internet: sin DTD ni entidades externas (XXE).
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return f.createXMLStreamReader(in); // detecta UTF-8 y el BOM solo
    }

    private static double numero(String s) {
        if (s == null || s.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
