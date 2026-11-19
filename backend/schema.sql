-- PrecioGas: esquema para TiDB (compatible con MySQL).
-- Ejecutar una vez en la consola SQL de TiDB Cloud o con el cliente mysql.

CREATE DATABASE IF NOT EXISTS preciogas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE preciogas;

-- Estaciones de la competencia (datos de la CNE). El permiso es único y sirve
-- para unir el archivo de estaciones con el de precios.
CREATE TABLE IF NOT EXISTS estacion (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    permiso     VARCHAR(64)  NOT NULL,
    nombre      VARCHAR(255) NOT NULL,
    marca       VARCHAR(120),
    latitud     DECIMAL(9,6) NOT NULL,
    longitud    DECIMAL(9,6) NOT NULL,
    municipio   VARCHAR(120),
    actualizado DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_estacion_permiso (permiso),
    KEY idx_estacion_municipio (municipio),
    KEY idx_estacion_coordenadas (latitud, longitud)
);

-- Historial de precios de la competencia: una fila por estación, combustible y fecha.
CREATE TABLE IF NOT EXISTS precio (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    estacion_id  BIGINT       NOT NULL,
    combustible  ENUM('magna','premium','diesel') NOT NULL,
    precio       DECIMAL(6,2) NOT NULL,
    fecha        DATE         NOT NULL,
    UNIQUE KEY uk_precio_dia (estacion_id, combustible, fecha),
    KEY idx_precio_fecha (fecha),
    CONSTRAINT fk_precio_estacion FOREIGN KEY (estacion_id) REFERENCES estacion (id)
);

-- Precios de nuestra gasolinera (actuales e historial).
CREATE TABLE IF NOT EXISTS mi_precio (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    combustible ENUM('magna','premium','diesel') NOT NULL,
    precio      DECIMAL(6,2) NOT NULL,
    fecha       DATE         NOT NULL,
    KEY idx_mi_precio_combustible_fecha (combustible, fecha)
);
