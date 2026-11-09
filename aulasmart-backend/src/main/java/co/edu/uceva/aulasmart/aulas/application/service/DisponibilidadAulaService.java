package co.edu.uceva.aulasmart.aulas.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.domain.exception.AulaNoEncontradaException;
import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import co.edu.uceva.aulasmart.aulas.domain.model.Aula;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaFiltro;
import co.edu.uceva.aulasmart.aulas.domain.model.Disponibilidad;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.aulas.domain.model.OcupacionAula;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disponibilidad de reservas. Solo afirma disponibilidad con el catálogo de
 * SIGA vigente y con la ocupación académica vigente, y excluye las aulas
 * ocupadas por la unión de: reservas APROBADAS de AulaSmart y eventos reales
 * del calendario público de SIGA (`horario_academico`).
 */
@Service
public class DisponibilidadAulaService {
    private final AulaConsultaPort aulaConsultaPort;
    private final SolicitudAprobadaPort solicitudAprobadaPort;
    private final HorarioAcademicoPort horarioAcademicoPort;
    private final SincronizacionSigaPort sincronizacionSigaPort;
    private final Clock clock;
    private final Duration maxAntiguedad;

    public DisponibilidadAulaService(
            AulaConsultaPort aulaConsultaPort,
            SolicitudAprobadaPort solicitudAprobadaPort,
            HorarioAcademicoPort horarioAcademicoPort,
            SincronizacionSigaPort sincronizacionSigaPort,
            Clock clock,
            @Value("${aulasmart.disponibilidad.max-antiguedad:PT24H}") Duration maxAntiguedad
    ) {
        this.aulaConsultaPort = aulaConsultaPort;
        this.solicitudAprobadaPort = solicitudAprobadaPort;
        this.horarioAcademicoPort = horarioAcademicoPort;
        this.sincronizacionSigaPort = sincronizacionSigaPort;
        this.clock = clock;
        this.maxAntiguedad = maxAntiguedad;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Disponibilidad consultar(AulaFiltro filtro, Intervalo intervalo) {
        Instant verificadoEn = clock.instant();
        Instant limiteActualizacion = verificadoEn.minus(maxAntiguedad);
        if (!sincronizacionSigaPort.catalogoAulasVigente(limiteActualizacion)
                || !sincronizacionSigaPort.eventosCubren(limiteActualizacion, intervalo)) {
            throw new DisponibilidadNoVerificableException();
        }

        Set<Long> ocupadas = new HashSet<>(
                solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(intervalo)
        );
        ocupadas.addAll(horarioAcademicoPort.buscarAulasConEventos(intervalo));
        List<Aula> disponibles = aulaConsultaPort.buscarActivas(filtro).stream()
                .filter(aula -> !ocupadas.contains(aula.id()))
                .toList();
        return new Disponibilidad(intervalo, verificadoEn, disponibles);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<OcupacionAula> consultarOcupacion(Long aulaId, Intervalo intervalo) {
        aulaConsultaPort.buscarActivaPorId(aulaId)
                .orElseThrow(() -> new AulaNoEncontradaException(aulaId));
        Instant limite = clock.instant().minus(maxAntiguedad);
        if (!sincronizacionSigaPort.catalogoAulasVigente(limite)
                || !sincronizacionSigaPort.eventosCubren(limite, intervalo)) {
            throw new DisponibilidadNoVerificableException();
        }
        var ocupaciones = new ArrayList<>(horarioAcademicoPort.buscarOcupacionDeAula(aulaId, intervalo));
        ocupaciones.addAll(solicitudAprobadaPort.buscarOcupacion(aulaId, intervalo));
        ocupaciones.sort(Comparator.comparing(OcupacionAula::inicio));
        return List.copyOf(ocupaciones);
    }
}
