package co.edu.uceva.aulasmart.aulabot.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Consulta de aula comprendida en lenguaje natural. Sin intervalo consulta catálogo;
 * con intervalo absoluto se valida como [inicio, fin), igual que en AulaSmart.
 */
public record ConsultaAulaBot(
        FiltroAulaBot filtro,
        Instant inicio,
        Instant fin,
        String textoOriginal
) {
    public ConsultaAulaBot {
        Objects.requireNonNull(filtro, "El filtro de la consulta es obligatorio.");
        if ((inicio == null) != (fin == null) || (inicio != null && !fin.isAfter(inicio))) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio.");
        }
        textoOriginal = textoOriginal == null ? "" : textoOriginal.strip();
    }
}
