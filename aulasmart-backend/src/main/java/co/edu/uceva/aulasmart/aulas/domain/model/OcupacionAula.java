package co.edu.uceva.aulasmart.aulas.domain.model;

import java.time.Instant;

/**
 * Vista de una franja ocupada de un aula (proveniente de SIGA) para la ficha
 * de detalles del aula: intervalo y detalle textual (clase/evento).
 */
public record OcupacionAula(
        Long idAula,
        String detalles,
        Instant inicio,
        Instant fin
) {
}
