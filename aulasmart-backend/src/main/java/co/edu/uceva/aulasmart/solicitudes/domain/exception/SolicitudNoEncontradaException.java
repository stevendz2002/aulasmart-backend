package co.edu.uceva.aulasmart.solicitudes.domain.exception;

public class SolicitudNoEncontradaException extends RuntimeException {
    public SolicitudNoEncontradaException(Long idSolicitud) {
        super("No existe una solicitud con el identificador " + idSolicitud + ".");
    }
}
