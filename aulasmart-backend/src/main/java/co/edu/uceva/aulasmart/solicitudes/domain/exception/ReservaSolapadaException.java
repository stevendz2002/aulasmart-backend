package co.edu.uceva.aulasmart.solicitudes.domain.exception;

/**
 * La base impide dos reservas aprobadas solapadas para el mismo aula
 * (restricción EXCLUDE de `solicitud_aula`); esta excepción da significado
 * a ese conflicto antes de tocar la base.
 */
public class ReservaSolapadaException extends RuntimeException {
    public ReservaSolapadaException(Long idAula) {
        super("El aula " + idAula + " ya tiene una reserva aprobada que se solapa con el intervalo solicitado.");
    }
}
