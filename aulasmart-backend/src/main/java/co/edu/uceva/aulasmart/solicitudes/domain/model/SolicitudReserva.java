package co.edu.uceva.aulasmart.solicitudes.domain.model;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;

import java.time.Instant;
import java.util.List;

/**
 * Solicitud de reserva de un aula hecha por un docente. Los datos de revisión
 * (revisor y momento) solo existen cuando la solicitud ya fue revisada;
 * mientras tanto su estado es `PENDIENTE`.
 */
public record SolicitudReserva(
        Long id,
        Long aulaId,
        Long docenteId,
        Intervalo intervalo,
        String estado,
        String motivo,
        Long revisadaPorId,
        Instant revisadaEn,
        Instant creadaEn
) {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";

    /**
     * Los estados posibles de una solicitud de reserva.
     */
    public static final List<String> ESTADOS = List.of(PENDIENTE, APROBADA, RECHAZADA);

    public boolean estaPendiente() {
        return PENDIENTE.equals(estado);
    }
}
