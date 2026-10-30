package co.edu.uceva.aulasmart.solicitudes.application.port;

import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;

import java.util.List;
import java.util.Optional;

public interface SolicitudReservaPort {
    SolicitudReserva guardar(SolicitudReserva solicitud);

    Optional<SolicitudReserva> buscarPorId(Long idSolicitud);

    List<SolicitudReserva> buscarPorDocente(Long docenteId);

    List<SolicitudReserva> buscarTodas();

    List<SolicitudReserva> buscarPorEstado(String estado);
}
