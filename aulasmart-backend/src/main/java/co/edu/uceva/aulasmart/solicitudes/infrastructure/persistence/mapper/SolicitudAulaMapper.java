package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.mapper;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.entity.SolicitudAulaEntity;

public final class SolicitudAulaMapper {
    private SolicitudAulaMapper() {
    }

    public static SolicitudReserva aDominio(SolicitudAulaEntity entity) {
        return new SolicitudReserva(
                entity.getId(),
                entity.getAulaId(),
                entity.getDocenteId(),
                new Intervalo(entity.getFechaInicio(), entity.getFechaFin()),
                entity.getEstado(),
                entity.getMotivo(),
                entity.getRevisadaPorId(),
                entity.getRevisadaEn(),
                entity.getCreadaEn()
        );
    }
}
