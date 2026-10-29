package co.edu.uceva.aulasmart.aulas.domain.model;

import co.edu.uceva.aulasmart.aulas.domain.exception.IntervaloInvalidoException;

import java.time.Instant;
import java.util.Objects;

public record Intervalo(Instant inicio, Instant fin) {
    public Intervalo {
        Objects.requireNonNull(inicio, "La fecha de inicio es obligatoria.");
        Objects.requireNonNull(fin, "La fecha de finalización es obligatoria.");
        if (!fin.isAfter(inicio)) {
            throw new IntervaloInvalidoException("La fecha de finalización debe ser posterior al inicio.");
        }
    }
}
