# Contrato de la API de PrecioGas

Este es el acuerdo entre el equipo de la API (Java) y el equipo de SwiftUI. Solo cambia si los cuatro están de acuerdo. Los valores son de ejemplo, no datos reales.

## Reglas generales

- Base: `http://<host>:8080/api` (en el simulador de Xcode: `http://localhost:8080/api`).
- JSON en UTF-8.
- Fechas en ISO 8601: `"2026-11-17T06:00:00Z"` para fecha y hora, `"2026-11-17"` para solo fecha.
- Precios como números en pesos por litro, con 2 decimales.
- Combustibles en minúsculas: `magna`, `premium`, `diesel`.
- Errores: `{"error": "mensaje"}` con código 400 (petición mal hecha), 404 (no existe) o 500 (falla del servidor). También pueden salir 405 (método equivocado) y 415 (falta `Content-Type: application/json`).
- Si un cuerpo JSON trae campos de más, se ignoran.

## GET /competencia

Estaciones cercanas con sus precios, ordenadas de la más cercana a la más lejana.

| Parámetro | Obligatorio | Por omisión |
|---|---|---|
| `lat`, `lng` | No | Ubicación de nuestra gasolinera |
| `radio` (km, mayor que 0 y hasta 100) | No | 5 |
| `combustible` | No | Sin filtro: todas las estaciones con todos sus precios |

Con `combustible`, solo vienen las estaciones que venden ese combustible. Una estación puede no tener los tres precios.

```
GET /competencia?lat=20.90&lng=-101.30&radio=5&combustible=magna
```

```json
{
  "actualizado": "2026-11-17T06:00:00Z",
  "estaciones": [
    {
      "id": 101,
      "permiso": "PL/0000/EXP/ES/2015",
      "nombre": "Gasolinera Ejemplo",
      "marca": "Marca Ejemplo",
      "lat": 20.91,
      "lng": -101.29,
      "distanciaKm": 1.8,
      "precios": { "magna": 23.69, "premium": 28.49, "diesel": 27.15 }
    }
  ]
}
```

## GET /resumen?combustible=magna

Promedio, mínimo y máximo de las estaciones a 5 km de nuestra gasolinera. Nuestra propia estación no entra al promedio. `diferencia` = mi precio menos el promedio (negativo = estoy más barato). Si `combustible` falta, se usa `magna`. Si no hay datos, los números vienen en `null`.

```json
{
  "combustible": "magna",
  "promedio": 23.70,
  "minimo": 22.90,
  "maximo": 24.60,
  "miPrecio": 23.50,
  "diferencia": -0.20,
  "estaciones": 12
}
```

## GET /mis-precios

Todos mis precios (actuales e historial), del más reciente al más antiguo. El precio actual de cada combustible es el primero que aparece.

```json
[ { "id": 1, "combustible": "magna", "precio": 23.50, "fecha": "2026-11-17" } ]
```

## POST /mis-precios

Recibe `{ "combustible": "magna", "precio": 23.50 }` y responde **201** con el registro creado y el encabezado `Location`. La fecha la pone el servidor (hora de Ciudad de México). El precio debe ser mayor que 0 y menor que 100.

## PUT /mis-precios/{id}

Recibe `{ "precio": 23.60 }` y responde **200** con el registro actualizado, o **404** si el id no existe.

## POST /admin/actualizar

Corre el colector de datos de la CNE y responde `{ "estaciones": <n>, "precios": <n> }`. Si el servidor tiene configurado `admin.token`, hay que mandar el encabezado `X-Admin-Token` (si falta o no coincide: 401).

## GET /salud

Para comprobar que la API está viva: `{ "estado": "ok", "fuente": "ejemplo" | "bd", "hora": "..." }`.

## Decisiones del 18 de noviembre

- Radio por omisión: 5 km.
- `/resumen` no incluye mi propia estación en el promedio.
- Estado o municipio que se carga: **pendiente** (depende del archivo de la CNE).
