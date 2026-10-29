package co.edu.uceva.aulasmart.aulas.presentation.dto;

import co.edu.uceva.aulasmart.aulas.domain.model.CatalogoAula;

public record CatalogoResponse(Long id, Long codigoSiga, String nombre) {
    static CatalogoResponse fromDomain(CatalogoAula catalogo) {
        return new CatalogoResponse(catalogo.id(), catalogo.codigoSiga(), catalogo.nombre());
    }
}
