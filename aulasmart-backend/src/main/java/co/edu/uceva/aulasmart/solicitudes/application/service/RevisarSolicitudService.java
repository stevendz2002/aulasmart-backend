package co.edu.uceva.aulasmart.solicitudes.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.application.port.VerificacionEventosSigaPort;
import co.edu.uceva.aulasmart.solicitudes.application.port.SolicitudReservaPort;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.ReservaSolapadaException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudNoEncontradaException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudYaRevisadaException;
import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

/**
 * Revisión de solicitudes por un revisor (ADMINISTRATIVO o ADMINISTRADOR):
 * APROBAR o RECHAZAR una solicitud pendiente dejando la auditoría de la
 * decisión. Al aprobar se re-verifica que no exista una reserva aprobada
 * solapada; la base (EXCLUDE) es la garantía final.
 */
@Service
public class RevisarSolicitudService {

    private static final String APROBAR = "APROBAR";
    private static final String RECHAZAR = "RECHAZAR";

    private final SolicitudReservaPort solicitudReservaPort;
    private final SolicitudAprobadaPort solicitudAprobadaPort;
    private final HorarioAcademicoPort horarioAcademicoPort;
    private final AulaConsultaPort aulaConsultaPort;
    private final VerificacionEventosSigaPort verificacionEventosSigaPort;
    private final Clock clock;

    public RevisarSolicitudService(
            SolicitudReservaPort solicitudReservaPort,
            SolicitudAprobadaPort solicitudAprobadaPort,
            HorarioAcademicoPort horarioAcademicoPort,
            AulaConsultaPort aulaConsultaPort,
            VerificacionEventosSigaPort verificacionEventosSigaPort,
            Clock clock
    ) {
        this.solicitudReservaPort = solicitudReservaPort;
        this.solicitudAprobadaPort = solicitudAprobadaPort;
        this.horarioAcademicoPort = horarioAcademicoPort;
        this.aulaConsultaPort = aulaConsultaPort;
        this.verificacionEventosSigaPort = verificacionEventosSigaPort;
        this.clock = clock;
    }

    @Transactional
    public SolicitudReserva revisar(Long idSolicitud, Long revisorId, String decision) {
        String decisionNormalizada = decision == null ? "" : decision.strip().toUpperCase(Locale.ROOT);
        String estadoDestino = switch (decisionNormalizada) {
            case APROBAR -> SolicitudReserva.APROBADA;
            case RECHAZAR -> SolicitudReserva.RECHAZADA;
            default -> throw new IllegalArgumentException(
                    "La decisión de revisión debe ser APROBAR o RECHAZAR; el valor recibido fue `"
                            + decision + "`.");
        };

        SolicitudReserva solicitud = solicitudReservaPort.buscarPorId(idSolicitud)
                .orElseThrow(() -> new SolicitudNoEncontradaException(idSolicitud));

        if (!solicitud.estaPendiente()) {
            throw new SolicitudYaRevisadaException(idSolicitud, solicitud.estado());
        }
        if (SolicitudReserva.APROBADA.equals(estadoDestino)) {
            if (solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(solicitud.intervalo())
                    .contains(solicitud.aulaId())) {
                throw new ReservaSolapadaException(solicitud.aulaId());
            }
            if (horarioAcademicoPort.buscarAulasConEventos(solicitud.intervalo())
                    .contains(solicitud.aulaId())) {
                throw new ReservaSolapadaException(solicitud.aulaId());
            }
            var aulaOpt = aulaConsultaPort.buscarActivaPorId(solicitud.aulaId());
            if (aulaOpt.isPresent() && verificacionEventosSigaPort.aulaTieneEventosEnSiga(aulaOpt.get().codigoSiga(), solicitud.intervalo())) {
                throw new ReservaSolapadaException(solicitud.aulaId());
            }
        }

        return solicitudReservaPort.guardar(new SolicitudReserva(
                solicitud.id(),
                solicitud.aulaId(),
                solicitud.docenteId(),
                solicitud.intervalo(),
                estadoDestino,
                solicitud.motivo(),
                revisorId,
                clock.instant(),
                solicitud.creadaEn()
        ));
    }
}
