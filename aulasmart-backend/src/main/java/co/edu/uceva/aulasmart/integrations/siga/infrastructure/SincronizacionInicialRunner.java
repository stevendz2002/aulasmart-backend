package co.edu.uceva.aulasmart.integrations.siga.infrastructure;

import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.integrations.siga.application.service.SincronizacionSigaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.List;

/** Recupera al arrancar la misma cobertura/coherencia que exige la disponibilidad. */
@Component
public class SincronizacionInicialRunner implements ApplicationRunner {
    private static final Logger REGISTRO = LoggerFactory.getLogger(SincronizacionInicialRunner.class);
    private final SincronizacionSigaService servicio;
    private final SincronizacionSigaPort estado;
    private final Clock clock;
    private final Duration maxAntiguedad;
    private final boolean habilitado;
    private final int diasVentana;

    public SincronizacionInicialRunner(SincronizacionSigaService servicio, SincronizacionSigaPort estado,
            Clock clock, @Value("${aulasmart.disponibilidad.max-antiguedad:PT24H}") Duration maxAntiguedad,
            @Value("${aulasmart.siga.sincronizacion-inicial.enabled:true}") boolean habilitado,
            @Value("${aulasmart.siga.eventos.dias-ventana:7}") int diasVentana) {
        this.servicio = servicio;
        this.estado = estado;
        this.clock = clock;
        this.maxAntiguedad = maxAntiguedad;
        this.habilitado = habilitado;
        this.diasVentana = diasVentana;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!habilitado) return;
        Instant limite = clock.instant().minus(maxAntiguedad);
        var zona = ZoneOffset.ofHours(-5);
        var hoy = LocalDate.now(clock.withZone(zona));
        var ventana = new Intervalo(hoy.atStartOfDay().toInstant(zona), hoy.plusDays(diasVentana).atStartOfDay().toInstant(zona));
        boolean aulasVigentes = estado.catalogoAulasVigente(limite);
        boolean eventosCubiertos = estado.eventosCubren(limite, ventana);
        if (aulasVigentes && eventosCubiertos) return;
        // Un catálogo renovado requiere eventos posteriores, aunque haya un éxito reciente.
        var recursos = aulasVigentes ? List.of("EVENTOS") : List.of("AULAS", "EVENTOS");
        try {
            for (var resultado : servicio.sincronizar(recursos)) {
                if ("EXITOSA".equals(resultado.estado())) {
                    REGISTRO.info("Sincronización inicial {} completada.", resultado.recurso());
                } else {
                    REGISTRO.warn("Sincronización inicial {}: {}. Recuperación pendiente del próximo ciclo automático.",
                            resultado.recurso(), resultado.estado());
                }
            }
        } catch (Exception e) {
            REGISTRO.warn("No fue posible completar la sincronización inicial. El próximo ciclo volverá a intentarlo.", e);
        }
    }
}
