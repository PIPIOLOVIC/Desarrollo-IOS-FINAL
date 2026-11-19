# PrecioGas API

API REST en Java para la app PrecioGas: Maven web, JAX-RS (Jersey 3.1), Jackson, Tomcat 10.1 y TiDB (driver MySQL).

**Estado actual:** los 6 endpoints del contrato y `/api/salud` responden con **datos de ejemplo en memoria** (no son datos reales). Lo que falta: conectar TiDB (`JdbcDao`) y el colector de la CNE.

## Requisitos

- Java 17 o superior (probado con Java 21).
- Tomcat **10.1** (usa `jakarta.*`; Tomcat 9 no sirve).
- Maven no es necesario: el proyecto trae Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Compilar y ejecutar

```powershell
cd backend
.\mvnw.cmd clean package
```

Esto corre las pruebas unitarias (JUnit 5, `.\mvnw.cmd test` para solo probar) y genera `target/ROOT.war`. Cópiala a la carpeta `webapps` de Tomcat y arranca Tomcat (`bin\startup.bat`). Queda en:

```
http://localhost:8080/api/salud
```

**Con NetBeans:** File → Open Project → carpeta `backend`, agrega Tomcat 10.1 en Servers y dale Run. El `context.xml` ya pone la ruta en la raíz.

**Desde la app iOS:** el simulador usa `http://localhost:8080/api`. En un iPhone real usa la IP de la PC (misma red) y la excepción de App Transport Security por ser `http://`.

## Configuración (base de datos)

1. Copia `config.example.properties` a `src/main/resources/config.properties` (ignorado por Git) y llena host, usuario y contraseña de TiDB Cloud. También funcionan variables de entorno: `PRECIOGAS_DB_URL`, `PRECIOGAS_DB_USER`, `PRECIOGAS_DB_PASSWORD`.
2. Corre `schema.sql` en la consola SQL de TiDB para crear `estacion`, `precio` y `mi_precio`.

Sin `db.url` la API usa los datos de ejemplo.

## Contrato

Base `http://<host>:8080/api`, JSON UTF-8, fechas ISO 8601, precios en pesos por litro, combustibles `magna`, `premium`, `diesel`. Errores: `{"error": "mensaje"}` con 400, 404 o 500.

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/salud` | Comprueba que la API está viva y qué fuente usa (`ejemplo` o `bd`) |
| GET | `/competencia?lat=&lng=&radio=&combustible=` | Estaciones cercanas ordenadas por distancia. Todo opcional: sin lat/lng usa mi gasolinera, radio 5 km, sin combustible trae todos los precios |
| GET | `/resumen?combustible=magna` | Promedio, mínimo, máximo de la zona (5 km) y diferencia de mi precio contra el promedio (negativo = más barato) |
| GET | `/mis-precios` | Mis precios, del más reciente al más antiguo |
| POST | `/mis-precios` | Cuerpo `{"combustible":"magna","precio":23.50}` → 201 con el registro |
| PUT | `/mis-precios/{id}` | Cuerpo `{"precio":23.60}` → 200 con el registro, 404 si no existe |
| POST | `/admin/actualizar` | Corre el colector → `{"estaciones":n,"precios":n}`. Si `admin.token` está configurado, pide el encabezado `X-Admin-Token` (401 si falta) |

Los cuerpos JSON pueden traer campos de más (por ejemplo `id` o `fecha`): se ignoran. La fecha de un precio nuevo la pone el servidor con la zona `America/Mexico_City`.

Decisiones tomadas (para confirmar con el equipo): radio por omisión 5 km, y `/resumen` **no** incluye mi propia estación en el promedio.

## Pruebas con curl

```powershell
curl.exe http://localhost:8080/api/competencia?combustible=magna
curl.exe http://localhost:8080/api/resumen?combustible=magna
curl.exe -X POST -H "Content-Type: application/json" -d '{\"combustible\":\"magna\",\"precio\":23.65}' http://localhost:8080/api/mis-precios
```

## Estructura (`com.preciogas.api`)

- `config`: `ApiApplication` (`/api`), configuración de Jackson y lectura de `config.properties`.
- `resource`: un recurso por endpoint y `ErrorMapper` para el formato de errores.
- `service`: `PreciosService` (filtro por radio con Haversine y cálculo del resumen).
- `dao`: interfaz `PreciosDao`; `EjemploDao` en memoria (luego `JdbcDao` para TiDB).
- `model`: `Estacion`, `MiPrecio`, `Resumen`, `Competencia`, etc.
- `collector`: colector de la CNE (pendiente).
