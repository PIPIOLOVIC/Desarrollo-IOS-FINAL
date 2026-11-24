# PrecioGas API

API REST en Java para la app PrecioGas: Maven web, JAX-RS (Jersey 3.1), Jackson, Tomcat 10.1 y TiDB (driver MySQL).

**Estado actual:** con base configurada, todos los endpoints leen y guardan en TiDB (`JdbcDao`) y `/salud` responde `"fuente":"bd"`. El colector de la CNE carga los datos reales (`POST /api/admin/actualizar`, ver abajo). Sin base, la API responde con datos de ejemplo en memoria (`"fuente":"ejemplo"`).

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

1. Copia `config.example.properties` a `src/main/resources/config.properties` (ignorado por Git) y llena `db.host`, `db.user`, `db.password` (TiDB Cloud: `db.port=4000`, `db.ssl=true`), `admin.token` y los datos de tu gasolinera (`mi.lat`, `mi.lng`, `mi.permiso`).
2. Corre `schema.sql` en la consola SQL de TiDB para crear `estacion`, `precio` y `mi_precio`.

Sin `db.host` (ni `db.url`) la API usa los datos de ejemplo. En código, la conexión se abre con `Db.abrir()` (paquete `config`); si falla, lanza un error claro sin host, usuario ni contraseña.

Las pruebas (`mvnw test`) **no** leen tu `config.properties`: usan `src/test/resources/config-pruebas.properties` (sin base), así que siempre corren con datos de ejemplo.

### Variables de entorno (Render)

Cada clave de `config.properties` se puede dar como variable `PRECIOGAS_` + la clave en mayúsculas con `_` en vez de punto. Las variables ganan sobre el archivo.

| Variable | Ejemplo / por omisión | Para qué |
|---|---|---|
| `PRECIOGAS_DB_HOST` | `gateway01.us-east-1.prod.aws.tidbcloud.com` | Host de TiDB (sin él: datos de ejemplo) |
| `PRECIOGAS_DB_PORT` | `4000` | Puerto |
| `PRECIOGAS_DB_NAME` | `preciogas` | Base de datos |
| `PRECIOGAS_DB_SSL` | `false` (en TiDB Cloud: `true`) | TLS con `sslMode=VERIFY_IDENTITY` |
| `PRECIOGAS_DB_USER` / `PRECIOGAS_DB_PASSWORD` | | Credenciales |
| `PRECIOGAS_ADMIN_TOKEN` | | Obligatorio con base para `/admin/actualizar` |
| `PRECIOGAS_MI_LAT` / `PRECIOGAS_MI_LNG` | `20.90` / `-101.30` (ejemplo) | Ubicación de nuestra gasolinera |
| `PRECIOGAS_MI_PERMISO` | | Permiso CNE de nuestra gasolinera (no se guarda como competencia) |
| `PRECIOGAS_CNE_RADIO_KM` | `100` | Radio de carga del colector |
| `PRECIOGAS_COLECTOR_PROGRAMADO` / `PRECIOGAS_COLECTOR_HORA` | `false` / `07:00` | Corrida diaria automática del colector |
| `PRECIOGAS_ZONA` | `America/Mexico_City` | Zona horaria de las fechas |

### Publicar en Render

Render no tiene Java nativo, así que usa el `Dockerfile` de esta carpeta (lo construye Render; no hace falta Docker en tu PC). La imagen no lleva `config.properties` (`.dockerignore`): todo llega por variables de entorno.

1. En Render: **New → Web Service**, conecta el repositorio, **Language: Docker**, **Root Directory: `backend`**.
2. En **Environment** agrega las variables de la tabla de arriba (al menos `PRECIOGAS_DB_HOST`, `PRECIOGAS_DB_SSL=true`, `PRECIOGAS_DB_USER`, `PRECIOGAS_DB_PASSWORD`, `PRECIOGAS_ADMIN_TOKEN`, `PRECIOGAS_MI_LAT`, `PRECIOGAS_MI_LNG`, `PRECIOGAS_MI_PERMISO`). El puerto lo pone Render (`PORT`).
3. Al terminar el despliegue: `https://<tu-servicio>.onrender.com/api/salud` debe responder `"fuente":"bd"`. Esa es la base que usa la app iOS (con `https://` no hace falta la excepción de App Transport Security).

En el plan gratuito el servicio se duerme tras 15 min sin uso y la primera petición tarda unos 50 s en despertarlo: abrir `/salud` antes de la demo. TiDB Cloud acepta conexiones desde cualquier IP solo si así está configurado en su panel (Networking).

## Colector de la CNE

Fuente: los XML públicos de la CNE (antes CRE) `https://publicacionexterna.azurewebsites.net/publicaciones/places` (estaciones: `place_id`, nombre/razón social, permiso `cre_id`, coordenadas) y `.../prices` (precios por `place_id`: `regular`, `premium`, `diesel`). No traen marca, municipio, estado ni fecha.

Qué hace `POST /api/admin/actualizar`:

1. Descarga los dos archivos (unos 3 MB cada uno, de 10 a 40 s). Si la descarga falla, usa el respaldo: `cne.archivo.places` / `cne.archivo.prices` si están configurados, o la copia incluida en la WAR (`src/main/resources/cne/*.xml.gz`, del 23 nov 2026).
2. Une estaciones y precios por `place_id` y mapea `regular` → `magna`.
3. Se queda con las estaciones a `cne.radio.km` (100 km) o menos de `mi.lat`/`mi.lng`, sin la propia (`mi.permiso`), y descarta precios fuera de 15–40 $/L (errores de captura).
4. Guarda en una transacción: `estacion` por permiso (`INSERT ... ON DUPLICATE KEY UPDATE`, con `actualizado = NOW()`) y una fila de `precio` por estación, combustible y día (historial). Correrlo dos veces el mismo día actualiza el precio de ese día; no duplica.
5. Responde `{"estaciones": n, "precios": n}` y deja en el log de Tomcat una línea `[Colector] origen=... descarga=...s parseo=...s carga=...s ...`.

Reglas:

- Con base configurada, `admin.token` es **obligatorio** (sin él: 500) y hay que mandar el encabezado `X-Admin-Token` (si no coincide: 401).
- Si `mi.lat`/`mi.lng` siguen en los valores de ejemplo (20.90/-101.30), no carga y responde 500 con el motivo.
- La fecha de los precios es el día de la carga (zona `America/Mexico_City`), porque la CNE no publica fecha.

```powershell
# Descargar de la CNE y cargar
curl.exe -X POST -H "X-Admin-Token: <tu token>" http://localhost:8080/api/admin/actualizar
# Usar el archivo de respaldo, sin descargar
curl.exe -X POST -H "X-Admin-Token: <tu token>" "http://localhost:8080/api/admin/actualizar?origen=archivo"
```

Para revisar la carga en la consola de TiDB:

```sql
SELECT COUNT(*) FROM estacion;
SELECT fecha, combustible, COUNT(*), ROUND(AVG(precio), 2) FROM precio GROUP BY fecha, combustible;
```

### Corrida diaria automática

Con `colector.programado=true` (y base configurada), al arrancar Tomcat se programa el colector todos los días a `colector.hora` (07:00 por omisión, zona `America/Mexico_City`). Lo hace `ColectorProgramado`, un `ServletContextListener` con `ScheduledExecutorService`. En el log de Tomcat aparece `[Colector] Programado todos los días a las 07:00 ...`. Si una corrida falla (por ejemplo, TiDB no responde), se registra y la del día siguiente corre igual.

Está apagado por omisión. Solo sirve si el servidor sigue encendido: en el plan gratuito de Render el servicio se apaga por inactividad, así que ahí se usa el endpoint manual.

## Contrato

Base `http://<host>:8080/api`, JSON UTF-8, fechas ISO 8601, precios en pesos por litro, combustibles `magna`, `premium`, `diesel`. Errores: `{"error": "mensaje"}` con 400, 404 o 500.

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/salud` | Comprueba que la API está viva y qué fuente usa (`ejemplo` o `bd`) |
| GET | `/competencia?lat=&lng=&radio=&combustible=` | Estaciones cercanas ordenadas por distancia. Todo opcional: sin lat/lng usa mi gasolinera, radio 10 km, sin combustible trae todos los precios |
| GET | `/resumen?combustible=magna` | Promedio, mínimo, máximo de la zona (10 km) y diferencia de mi precio contra el promedio (negativo = más barato) |
| GET | `/mis-precios` | Mis precios, del más reciente al más antiguo |
| POST | `/mis-precios` | Cuerpo `{"combustible":"magna","precio":23.50}` → 201 con el registro |
| PUT | `/mis-precios/{id}` | Cuerpo `{"precio":23.60}` → 200 con el registro, 404 si no existe |
| POST | `/admin/actualizar` | Corre el colector → `{"estaciones":n,"precios":n}`. Pide el encabezado `X-Admin-Token` si `admin.token` está configurado (401 si falta); con base, `admin.token` es obligatorio. `?origen=archivo` usa el respaldo |

Los cuerpos JSON pueden traer campos de más (por ejemplo `id` o `fecha`): se ignoran. La fecha de un precio nuevo la pone el servidor con la zona `America/Mexico_City`.

Decisiones tomadas (para confirmar con el equipo): radio por omisión 10 km, y `/resumen` **no** incluye mi propia estación en el promedio.

## Pruebas con curl

```powershell
curl.exe http://localhost:8080/api/competencia?combustible=magna
curl.exe http://localhost:8080/api/resumen?combustible=magna
curl.exe -X POST -H "Content-Type: application/json" -d '{\"combustible\":\"magna\",\"precio\":23.65}' http://localhost:8080/api/mis-precios
```

## Estructura (`com.preciogas.api`)

- `config`: `ApiApplication` (`/api`), configuración de Jackson, lectura de `config.properties` (`Config`) y conexión a la base (`Db`).
- `resource`: un recurso por endpoint y `ErrorMapper` para el formato de errores.
- `service`: `PreciosService` (filtro por radio con Haversine y cálculo del resumen).
- `dao`: interfaz `PreciosDao`; `JdbcDao` (TiDB) y `EjemploDao` (en memoria, sin base). `Daos` elige uno al arrancar.
- `model`: `Estacion`, `MiPrecio`, `Resumen`, `Competencia`, etc.
- `collector`: colector de la CNE: `CneClient` (descarga), `CneParser` (StAX), `Colector` (une, filtra, respaldo), `CargaCne` (guarda en TiDB) y `ColectorProgramado` (corrida diaria).
