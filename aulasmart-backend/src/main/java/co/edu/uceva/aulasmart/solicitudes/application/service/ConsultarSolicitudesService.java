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

    public ConsultarSolicitudesService(SolicitudReservaPort solicitudReservaPort) {
        this.solicitudReservaPort = solicitudReservaPort;
    }

    @Transactional(readOnly = true)
    public List<SolicitudReserva> consultar(PrincipalDeSolicitudes.UsuarioActivo usuario, String estado) {
        String estadoNormalizado = normalizarEstado(estado);
        if (usuario.esDocente()) {
            List<SolicitudReserva> propias = solicitudReservaPort.buscarPorDocente(usuario.id());
            if (estadoNormalizado == null) {
                return propias;
            }
            return propias.stream()
                    .filter(solicitud -> estadoNormalizado.equals(solicitud.estado()))
                    .toList();
        }
        return estadoNormalizado == null
                ? solicitudReservaPort.buscarTodas()
                : solicitudReservaPort.buscarPorEstado(estadoNormalizado);
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
