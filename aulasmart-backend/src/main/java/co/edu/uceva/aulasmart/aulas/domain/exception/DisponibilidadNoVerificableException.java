package co.edu.uceva.aulasmart.aulas.domain.exception;

public class DisponibilidadNoVerificableException extends RuntimeException {
    public DisponibilidadNoVerificableException() {
        super("No se puede confirmar la disponibilidad porque la sincronización está incompleta, desactualizada o no cubre el intervalo consultado.");
    }
}
