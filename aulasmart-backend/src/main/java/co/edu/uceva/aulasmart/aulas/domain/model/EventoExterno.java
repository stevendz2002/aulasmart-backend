package co.edu.uceva.aulasmart.aulas.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Ocupación observada de un aula en el calendario público de SIGA: una
 * franja real (clase, reserva o evento institucional) durante la cual el
 * aula NO está disponible para reservar. Llega por la sincronización de
 * eventos; el identificador externo permite rastrear la ocurrencia en SIGA.
 */
public record EventoExterno(
        Long codigoAulaSiga,
        String idExterno,
        Instant inicio,
        Instant fin
) {
    public EventoExterno {
        Objects.requireNonNull(codigoAulaSiga, "El código SIGA del aula del evento es obligatorio.");
        Objects.requireNonNull(idExterno, "El identificador externo del evento es obligatorio.");
        Objects.requireNonNull(inicio, "La fecha de inicio del evento es obligatoria.");
        Objects.requireNonNull(fin, "La fecha de finalización del evento es obligatoria.");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La ocurrencia del evento debe terminar después de su inicio.");
        }
    }
}
