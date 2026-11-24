package com.preciogas.api.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexión JDBC a TiDB (o MySQL local), con los datos de Config:
 *  - db.url: si existe, se usa tal cual (compatibilidad).
 *  - si no: db.host, db.port (4000), db.name (preciogas) y db.ssl (false).
 *  - db.user y db.password.
 * Cada valor también se lee como variable de entorno: PRECIOGAS_DB_HOST, PRECIOGAS_DB_SSL, etc.
 * TiDB Cloud exige TLS: db.ssl=true usa sslMode=VERIFY_IDENTITY.
 *
 * Quien llama cierra la conexión (try-with-resources).
 */
public final class Db {

    private Db() {
    }

    /** Abre una conexión nueva. Si falla, lanza IllegalStateException sin host, usuario ni contraseña. */
    public static Connection abrir() {
        try {
            // Dentro de Tomcat el driver de WEB-INF/lib no siempre se registra solo en DriverManager.
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url(), Config.get("db.user", null), Config.get("db.password", null));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No está el driver de MySQL (mysql-connector-j) en el classpath");
        } catch (SQLException e) {
            // Sin la excepción original: su mensaje puede traer el host o el usuario y ErrorMapper la registraría.
            throw new IllegalStateException("No se pudo conectar a la base de datos (SQLState " + e.getSQLState()
                    + ", código " + e.getErrorCode() + "). Revisa db.* en config.properties o PRECIOGAS_DB_*.");
        }
    }

    static String url() {
        String url = Config.get("db.url", null);
        if (url != null) {
            return url;
        }
        String host = Config.get("db.host", null);
        if (host == null) {
            throw new IllegalStateException("No hay base de datos configurada: falta db.host (o PRECIOGAS_DB_HOST)");
        }
        return url(host, Config.get("db.port", "4000"), Config.get("db.name", "preciogas"),
                Boolean.parseBoolean(Config.get("db.ssl", "false")));
    }

    static String url(String host, String puerto, String base, boolean ssl) {
        return "jdbc:mysql://" + host + ":" + puerto + "/" + base
                + (ssl ? "?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3" : "?useSSL=false")
                + "&useUnicode=true&characterEncoding=utf-8&rewriteBatchedStatements=true";
    }
}
