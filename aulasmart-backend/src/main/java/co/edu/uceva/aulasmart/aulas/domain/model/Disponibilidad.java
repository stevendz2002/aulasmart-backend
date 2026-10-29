package co.edu.uceva.aulasmart.aulas.domain.model;

import java.time.Instant;
import java.util.List;

public record Disponibilidad(Intervalo intervalo, Instant verificadoEn, List<Aula> aulas) {
    public Disponibilidad {
        aulas = List.copyOf(aulas);
    }
}
