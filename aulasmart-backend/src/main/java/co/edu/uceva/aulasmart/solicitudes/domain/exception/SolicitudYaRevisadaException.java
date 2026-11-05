package co.edu.uceva.aulasmart.solicitudes.domain.exception;

public class SolicitudYaRevisadaException extends RuntimeException {
    public SolicitudYaRevisadaException(Long idSolicitud, String estadoActual) {
        super("La solicitud " + idSolicitud + " ya fue revisada (estado " + estadoActual + ").");
    }
}
