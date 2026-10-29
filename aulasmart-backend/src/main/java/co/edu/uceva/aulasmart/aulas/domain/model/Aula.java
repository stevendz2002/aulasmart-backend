package co.edu.uceva.aulasmart.aulas.domain.model;

public record Aula(
        Long id,
        Long codigoSiga,
        String nombre,
        CatalogoAula edificio,
        int capacidad,
        CatalogoAula tipoAula,
        CatalogoAula dependencia,
        boolean activa
) {
}
