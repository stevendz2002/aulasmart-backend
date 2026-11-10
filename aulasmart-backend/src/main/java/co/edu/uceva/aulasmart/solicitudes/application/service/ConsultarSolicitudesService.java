package co.edu.uceva.aulasmart.solicitudes.application.service;

import co.edu.uceva.aulasmart.solicitudes.application.port.SolicitudReservaPort;
import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Consulta de solicitudes con filtro opcional por estado. Un docente solo ve
 * sus propias solicitudes; un revisor (ADMINISTRATIVO o ADMINISTRADOR) ve
 * todas las de la plataforma.
 */
@Service
public class ConsultarSolicitudesService {

    private final SolicitudReservaPort solicitudReservaPort;
    private final java.time.Clock clock;

    public ConsultarSolicitudesService(SolicitudReservaPort solicitudReservaPort, java.time.Clock clock) {
        this.solicitudReservaPort = solicitudReservaPort;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SolicitudReserva> consultar(PrincipalDeSolicitudes.UsuarioActivo usuario, String estado) {
        String estadoNormalizado = normalizarEstado(estado);
        var solicitudes = usuario.esDocente()
                ? solicitudReservaPort.buscarPorDocente(usuario.id()) : solicitudReservaPort.buscarTodas();
        var ahora = clock.instant();
        var ordenadas = solicitudes.stream().map(s -> s.estaPendiente() && !s.intervalo().inicio().isAfter(ahora)
                ? new SolicitudReserva(s.id(), s.aulaId(), s.docenteId(), s.intervalo(), SolicitudReserva.RECHAZADA,
                    s.motivo(), null, s.intervalo().inicio(), s.creadaEn()) : s)
                .sorted(java.util.Comparator.comparing(SolicitudReserva::creadaEn)
                    .thenComparing(SolicitudReserva::id).reversed()).toList();
        var historial = ordenadas.stream().filter(s -> SolicitudReserva.RECHAZADA.equals(s.estado())
                || !s.intervalo().fin().isAfter(ahora)).limit(50).toList();
        return ordenadas.stream().filter(s -> historial.contains(s)
                || (!SolicitudReserva.RECHAZADA.equals(s.estado()) && s.intervalo().fin().isAfter(ahora)))
                .filter(s -> estadoNormalizado == null || estadoNormalizado.equals(s.estado())).toList();
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String normalizado = estado.strip().toUpperCase(Locale.ROOT);
        if (!SolicitudReserva.ESTADOS.contains(normalizado)) {
            throw new IllegalArgumentException(
                    "Estado desconocido: `" + estado + "`. Estados válidos: "
                            + String.join(", ", SolicitudReserva.ESTADOS) + ".");
        }
        return normalizado;
    }
}
