package com.preciogas.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DbTest {

    @Test
    void conTlsUsaVerifyIdentity() {
        assertEquals("jdbc:mysql://gateway.tidb:4000/preciogas"
                + "?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3"
                + "&useUnicode=true&characterEncoding=utf-8&rewriteBatchedStatements=true",
                Db.url("gateway.tidb", "4000", "preciogas", true));
    }

    @Test
    void sinTlsDesactivaSsl() {
        assertEquals("jdbc:mysql://127.0.0.1:3306/preciogas"
                + "?useSSL=false&useUnicode=true&characterEncoding=utf-8&rewriteBatchedStatements=true",
                Db.url("127.0.0.1", "3306", "preciogas", false));
    }
}
