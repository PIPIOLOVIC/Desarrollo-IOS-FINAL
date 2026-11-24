package com.preciogas.api.collector;

import com.preciogas.api.config.Config;
import com.preciogas.api.model.ResultadoActualizacion;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Corre el colector de la CNE una vez al día, a la hora colector.hora (07:00 por omisión,
 * zona de Config), mientras Tomcat esté encendido.
 *
 * Apagado por omisión: se activa con colector.programado=true y solo si hay base configurada.
 * En hostings que apagan el servidor por inactividad (Render gratis) no corre; ahí se usa
 * POST /api/admin/actualizar a mano.
 */
@WebListener
public class ColectorProgramado implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(ColectorProgramado.class.getName());
    private static final LocalTime HORA_POR_OMISION = LocalTime.of(7, 0);

    private ScheduledExecutorService reloj;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (!Boolean.parseBoolean(Config.get("colector.programado", "false"))) {
            LOG.info("[Colector] Programación diaria apagada (colector.programado=false)");
            return;
        }
        if (!Config.hayBaseDeDatos()) {
            LOG.warning("[Colector] colector.programado=true pero no hay base configurada; no se programa");
            return;
        }
        LocalTime hora = hora();
        long espera = segundosHasta(hora, ZonedDateTime.now(Config.zona()));
        reloj = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "colector-diario");
            t.setDaemon(true);
            return t;
        });
        // shortcut: cada 24 h exactas; si la zona tuviera horario de verano se correría una hora. México no lo usa desde 2022.
        reloj.scheduleAtFixedRate(ColectorProgramado::correr, espera, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
        LOG.info("[Colector] Programado todos los días a las " + hora + " (" + Config.zona() + "); el primero en "
                + Duration.ofSeconds(espera).toMinutes() + " min");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (reloj != null) {
            reloj.shutdownNow();
        }
    }

    private static void correr() {
        try {
            ResultadoActualizacion r = Colector.ejecutar(false);
            LOG.info("[Colector] Corrida diaria terminada: " + r.estaciones() + " estaciones, " + r.precios() + " precios");
        } catch (RuntimeException e) {
            // Sin este catch, una falla (p. ej. TiDB caída) cancelaría todas las corridas siguientes.
            LOG.warning("[Colector] Falló la corrida diaria: " + e.getMessage());
        }
    }

    private static LocalTime hora() {
        String valor = Config.get("colector.hora", null);
        if (valor == null) {
            return HORA_POR_OMISION;
        }
        try {
            return LocalTime.parse(valor);
        } catch (DateTimeParseException e) {
            LOG.warning("[Colector] colector.hora no válida (" + valor + ", usa HH:mm); se usa " + HORA_POR_OMISION);
            return HORA_POR_OMISION;
        }
    }

    /** Segundos desde ahora hasta la próxima vez que el reloj marque esa hora (hoy o mañana). */
    static long segundosHasta(LocalTime hora, ZonedDateTime ahora) {
        ZonedDateTime siguiente = ahora.with(hora);
        if (!siguiente.isAfter(ahora)) {
            siguiente = siguiente.plusDays(1);
        }
        return Duration.between(ahora, siguiente).getSeconds();
    }
}
