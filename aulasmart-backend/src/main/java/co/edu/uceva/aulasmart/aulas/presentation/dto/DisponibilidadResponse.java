package co.edu.uceva.aulasmart.aulas.presentation.dto;

import co.edu.uceva.aulasmart.aulas.domain.model.Disponibilidad;

import java.time.Instant;
import java.util.List;

public record DisponibilidadResponse(
        Instant inicio,
        Instant fin,
        Instant verificadoEn,
        List<AulaResponse> aulas
) {
    public static DisponibilidadResponse fromDomain(Disponibilidad disponibilidad) {
        return new DisponibilidadResponse(
                disponibilidad.intervalo().inicio(),
                disponibilidad.intervalo().fin(),
                disponibilidad.verificadoEn(),
                disponibilidad.aulas().stream().map(AulaResponse::fromDomain).toList()
        );
    }
}
