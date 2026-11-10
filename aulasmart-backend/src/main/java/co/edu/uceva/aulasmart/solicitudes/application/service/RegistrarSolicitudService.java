package co.edu.uceva.aulasmart.solicitudes.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.application.port.VerificacionEventosSigaPort;
import co.edu.uceva.aulasmart.aulas.domain.exception.AulaNoEncontradaException;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.solicitudes.application.port.SolicitudReservaPort;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.ReservaSolapadaException;
import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Registro de una solicitud de reserva de aula. El docente propone un aula,
 * un intervalo `[inicio, fin)` y un motivo; la solicitud queda PENDIENTE.
 * Antes de guardar se verifica que el aula exista y esté activa y que no
 * exista una reserva aprobada solapada.
 */
@Service
public class RegistrarSolicitudService {

    private final SolicitudReservaPort solicitudReservaPort;
    private final AulaConsultaPort aulaConsultaPort;
    private final SolicitudAprobadaPort solicitudAprobadaPort;
    private final HorarioAcademicoPort horarioAcademicoPort;
    private final VerificacionEventosSigaPort verificacionEventosSigaPort;
    private final Clock clock;

    public RegistrarSolicitudService(
            SolicitudReservaPort solicitudReservaPort,
            AulaConsultaPort aulaConsultaPort,
            SolicitudAprobadaPort solicitudAprobadaPort,
            HorarioAcademicoPort horarioAcademicoPort,
            VerificacionEventosSigaPort verificacionEventosSigaPort,
            Clock clock
    ) {
        this.solicitudReservaPort = solicitudReservaPort;
        this.aulaConsultaPort = aulaConsultaPort;
        this.solicitudAprobadaPort = solicitudAprobadaPort;
        this.horarioAcademicoPort = horarioAcademicoPort;
        this.verificacionEventosSigaPort = verificacionEventosSigaPort;
        this.clock = clock;
    }

    @Transactional
    public SolicitudReserva registrar(Long docenteId, Long aulaId, Instant inicio, Instant fin, String motivo) {
        if (aulaId == null) {
            throw new IllegalArgumentException("El aula de la solicitud es obligatoria.");
        }
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException(
                    "La fecha de inicio y la fecha de finalización son obligatorias.");
        }
        String motivoLimpio = motivo == null ? null : motivo.strip();
        if (motivoLimpio == null || motivoLimpio.isEmpty()) {
            throw new IllegalArgumentException("El motivo de la solicitud es obligatorio.");
        }
        var intervalo = new Intervalo(inicio, fin);
        if (!inicio.isAfter(clock.instant())) {
            throw new IllegalArgumentException("La reserva debe comenzar en el futuro.");
        }

        var aula = aulaConsultaPort.buscarActivaPorId(aulaId)
                .orElseThrow(() -> new AulaNoEncontradaException(aulaId));
        if (solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(intervalo).contains(aulaId)) {
            throw new ReservaSolapadaException(aulaId);
        }
        if (horarioAcademicoPort.buscarAulasConEventos(intervalo).contains(aulaId)) {
            throw new ReservaSolapadaException(aulaId);
        }
        if (verificacionEventosSigaPort.aulaTieneEventosEnSiga(aula.codigoSiga(), intervalo)) {
            throw new ReservaSolapadaException(aulaId);
        }

        return solicitudReservaPort.guardar(new SolicitudReserva(
                null,
                aulaId,
                docenteId,
                intervalo,
                SolicitudReserva.PENDIENTE,
                motivoLimpio,
                null,
                null,
                clock.instant()
        ));
    }
}
