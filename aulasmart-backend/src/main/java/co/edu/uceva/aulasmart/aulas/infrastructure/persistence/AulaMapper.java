package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import co.edu.uceva.aulasmart.aulas.domain.model.Aula;
import co.edu.uceva.aulasmart.aulas.domain.model.CatalogoAula;

final class AulaMapper {
    private AulaMapper() {
    }

    static Aula toDomain(AulaEntity entity) {
        return new Aula(
                entity.getId(),
                entity.getCodigoSiga(),
                entity.getNombre(),
                new CatalogoAula(
                        entity.getEdificio().getId(),
                        entity.getEdificio().getCodigoSiga(),
                        entity.getEdificio().getNombre()
                ),
                entity.getCapacidad(),
                new CatalogoAula(
                        entity.getTipoAula().getId(),
                        entity.getTipoAula().getCodigoSiga(),
                        entity.getTipoAula().getNombre()
                ),
                new CatalogoAula(
                        entity.getDependencia().getId(),
                        entity.getDependencia().getCodigoSiga(),
                        entity.getDependencia().getNombre()
                ),
                entity.isActiva()
        );
    }
}
