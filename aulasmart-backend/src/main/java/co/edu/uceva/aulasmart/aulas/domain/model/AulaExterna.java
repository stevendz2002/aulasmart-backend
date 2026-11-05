package co.edu.uceva.aulasmart.aulas.domain.model;

/**
 * Descripción de un aula según la fuente externa SIGA. Contiene únicamente los
 * nueve campos estructurados verificados en las respuestas reales; los campos
 * `view` e `iconcls` se descartan a propósito.
 */
public record AulaExterna(
        Long codigoAula,
        String nombreAula,
        Long codigoEdificio,
        String nombreEdificio,
        int sillas,
        Long codigoDependencia,
        String nombreDependencia,
        Long codigoTipoAula,
        String nombreTipoAula
) {
}
