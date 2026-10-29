package co.edu.uceva.aulasmart.integrations.siga.infrastructure;

import co.edu.uceva.aulasmart.integrations.siga.application.service.SincronizacionSigaService;
import co.edu.uceva.aulasmart.integrations.siga.application.service.SincronizadorDeAulasService;
import co.edu.uceva.aulasmart.integrations.siga.application.service.SincronizadorDeEventosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tarea programada de sincronización diaria. Una falla nunca detiene la
 * aplicación: el resultado queda evidenciado en `sincronizacion_siga`.
 */
@Component
public class TareaSincronizacionProgramada {

    private static final Logger REGISTRO = LoggerFactory.getLogger(TareaSincronizacionProgramada.class);

    private final SincronizacionSigaService servicio;

    public TareaSincronizacionProgramada(SincronizacionSigaService servicio) {
        this.servicio = servicio;
    }

    @Scheduled(
            cron = "${aulasmart.siga.programado.cron:0 0 4 * * *}",
            zone = "${aulasmart.siga.programado.zona:America/Bogota}"
    )
    public void sincronizarCadaDia() {
        try {
            servicio.sincronizar(List.of(
                    SincronizadorDeAulasService.RECURSO,
                    SincronizadorDeEventosService.RECURSO
            ));
        } catch (Exception e) {
            REGISTRO.error("La sincronización programada terminó con error.", e);
        }
    }
}
