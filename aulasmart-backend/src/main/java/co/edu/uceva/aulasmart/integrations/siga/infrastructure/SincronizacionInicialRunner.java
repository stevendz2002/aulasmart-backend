package co.edu.uceva.aulasmart.integrations.siga.infrastructure;

import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.integrations.siga.application.service.SincronizacionSigaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Disparador de sincronización inicial al arranque del backend.
 * Si la base de datos no cuenta con un catálogo de aulas o eventos vigentes
 * (por ejemplo, en una instalación limpia o base de datos recién creada),
 * ejecuta automáticamente la sincronización con SIGA para que las aulas
 * y la ocupación queden disponibles inmediatamente sin requerir sincronización manual.
 */
@Component
public class SincronizacionInicialRunner implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(SincronizacionInicialRunner.class);

    private final SincronizacionSigaService sincronizacionService;
    private final SincronizacionSigaPort sincronizacionSigaPort;
    private final Clock clock;
    private final Duration maxAntiguedad;
    private final boolean habilitado;

    public SincronizacionInicialRunner(
            SincronizacionSigaService sincronizacionService,
            SincronizacionSigaPort sincronizacionSigaPort,
            Clock clock,
            @Value("${aulasmart.disponibilidad.max-antiguedad:PT24H}") Duration maxAntiguedad,
            @Value("${aulasmart.siga.sincronizacion-inicial.enabled:true}") boolean habilitado
    ) {
        this.sincronizacionService = sincronizacionService;
        this.sincronizacionSigaPort = sincronizacionSigaPort;
        this.clock = clock;
        this.maxAntiguedad = maxAntiguedad;
        this.habilitado = habilitado;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            REGISTRO.info("Sincronización inicial automática deshabilitada por configuración.");
            return;
        }

        Instant ahora = clock.instant();
        Instant limiteVigencia = ahora.minus(maxAntiguedad);

        boolean aulasVigentes = sincronizacionSigaPort.catalogoAulasVigente(limiteVigencia);
        boolean eventosVigentes = sincronizacionSigaPort.eventosVigente(limiteVigencia);

        if (aulasVigentes && eventosVigentes) {
            REGISTRO.info("Catálogo de aulas y eventos vigentes en base de datos. No se requiere sincronización inicial.");
            return;
        }

        REGISTRO.info("Detectada ausencia de sincronización vigente con SIGA (aulas={}, eventos={}). Sincronizando automáticamente...",
                aulasVigentes, eventosVigentes);

        if (!aulasVigentes) {
            try {
                REGISTRO.info("Sincronizando catálogo de aulas desde SIGA...");
                sincronizacionService.ejecutar("AULAS");
                REGISTRO.info("Catálogo de aulas sincronizado exitosamente al arranque.");
            } catch (Exception e) {
                REGISTRO.warn("No fue posible completar la sincronización automática de AULAS: {}", e.getMessage());
            }
        }

        if (!eventosVigentes) {
            try {
                REGISTRO.info("Sincronizando eventos y ocupación académica desde SIGA...");
                sincronizacionService.ejecutar("EVENTOS");
                REGISTRO.info("Eventos académicos sincronizados exitosamente al arranque.");
            } catch (Exception e) {
                REGISTRO.warn("No fue posible completar la sincronización automática de EVENTOS: {}", e.getMessage());
            }
        }
    }
}

