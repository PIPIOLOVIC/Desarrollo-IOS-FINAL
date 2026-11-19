package com.preciogas.api.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * Punto de entrada de JAX-RS: todos los endpoints quedan bajo /api.
 */
@ApplicationPath("/api")
public class ApiApplication extends ResourceConfig {

    public ApiApplication() {
        packages("com.preciogas.api");
        // Sin los mappers de Jackson, para que un JSON mal formado salga como {"error": ...}
        register(JacksonFeature.withoutExceptionMappers());
        // No publicar /api/application.wadl
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
    }
}
