package co.edu.uceva.aulasmart.solicitudes.presentation.dto;

import java.time.Instant;

/**
 * Cuerpo del registro de una solicitud de reserva: aula, intervalo
 * `[inicio, fin)` en ISO-8601 y motivo.
 */
public record SolicitudRequest(Long aulaId, Instant inicio, Instant fin, String motivo) {
}
