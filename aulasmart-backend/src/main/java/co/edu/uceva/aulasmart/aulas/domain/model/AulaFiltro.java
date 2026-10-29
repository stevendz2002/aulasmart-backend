package co.edu.uceva.aulasmart.aulas.domain.model;

public record AulaFiltro(
        Long idEdificio,
        Long idTipoAula,
        Long idDependencia,
        Integer capacidadMinima
) {
    public AulaFiltro {
        if (capacidadMinima != null && capacidadMinima <= 0) {
            throw new IllegalArgumentException("La capacidad mínima debe ser mayor que cero.");
        }
    }
}
