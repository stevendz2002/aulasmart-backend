package co.edu.uceva.aulasmart.aulas.presentation.dto;

import java.time.Instant;

/** Ocupación de un aula en el calendario académico de SIGA (ficha de detalles). */
public record OcupacionResponse(
        Instant inicio,
        Instant fin,
        String detalles
) {
}
