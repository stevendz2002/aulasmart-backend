package co.edu.uceva.aulasmart.solicitudes.presentation.dto;

import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;

import java.time.Instant;

public record SolicitudResponse(
        Long id,
        Long aulaId,
        Long docenteId,
        Instant inicio,
        Instant fin,
        String estado,
        String motivo,
        Long revisadaPorId,
        Instant revisadaEn,
        Instant creadaEn
) {

    public static SolicitudResponse desde(SolicitudReserva solicitud) {
        return new SolicitudResponse(
                solicitud.id(),
                solicitud.aulaId(),
                solicitud.docenteId(),
                solicitud.intervalo().inicio(),
                solicitud.intervalo().fin(),
                solicitud.estado(),
                solicitud.motivo(),
                solicitud.revisadaPorId(),
                solicitud.revisadaEn(),
                solicitud.creadaEn()
        );
    }
}
