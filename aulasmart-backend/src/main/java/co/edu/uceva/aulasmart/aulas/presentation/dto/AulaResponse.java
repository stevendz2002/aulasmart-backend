package co.edu.uceva.aulasmart.aulas.presentation.dto;

import co.edu.uceva.aulasmart.aulas.domain.model.Aula;

public record AulaResponse(
        Long id,
        Long codigoSiga,
        String nombre,
        CatalogoResponse edificio,
        int capacidad,
        CatalogoResponse tipoAula,
        CatalogoResponse dependencia
) {
    public static AulaResponse fromDomain(Aula aula) {
        return new AulaResponse(
                aula.id(),
                aula.codigoSiga(),
                aula.nombre(),
                CatalogoResponse.fromDomain(aula.edificio()),
                aula.capacidad(),
                CatalogoResponse.fromDomain(aula.tipoAula()),
                CatalogoResponse.fromDomain(aula.dependencia())
        );
    }
}
