package co.edu.uceva.aulasmart.aulas.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import co.edu.uceva.aulasmart.aulas.domain.model.Aula;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaFiltro;
import co.edu.uceva.aulasmart.aulas.domain.model.Disponibilidad;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Disponibilidad de reservas. Solo afirma disponibilidad con el catálogo de
 * SIGA vigente y excluye las aulas reservadas mediante solicitudes APROBADAS.
 */
@Service
public class DisponibilidadAulaService {
    private final AulaConsultaPort aulaConsultaPort;
    private final SolicitudAprobadaPort solicitudAprobadaPort;
    private final SincronizacionSigaPort sincronizacionSigaPort;
    private final Clock clock;
    private final Duration maxAntiguedad;

    public DisponibilidadAulaService(
            AulaConsultaPort aulaConsultaPort,
            SolicitudAprobadaPort solicitudAprobadaPort,
            SincronizacionSigaPort sincronizacionSigaPort,
            Clock clock,
            @Value("${aulasmart.disponibilidad.max-antiguedad:PT24H}") Duration maxAntiguedad
    ) {
        this.aulaConsultaPort = aulaConsultaPort;
        this.solicitudAprobadaPort = solicitudAprobadaPort;
        this.sincronizacionSigaPort = sincronizacionSigaPort;
        this.clock = clock;
        this.maxAntiguedad = maxAntiguedad;
    }

    @Transactional(readOnly = true)
    public Disponibilidad consultar(AulaFiltro filtro, Intervalo intervalo) {
        Instant verificadoEn = clock.instant();
        Instant limiteActualizacion = verificadoEn.minus(maxAntiguedad);
        if (!sincronizacionSigaPort.catalogoAulasVigente(limiteActualizacion)) {
            throw new DisponibilidadNoVerificableException();
        }

        Set<Long> ocupadas = solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(intervalo);
        List<Aula> disponibles = aulaConsultaPort.buscarActivas(filtro).stream()
                .filter(aula -> !ocupadas.contains(aula.id()))
                .toList();
        return new Disponibilidad(intervalo, verificadoEn, disponibles);
    }
}
