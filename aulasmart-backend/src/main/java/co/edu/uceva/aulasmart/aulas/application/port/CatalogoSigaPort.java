package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.AulaExterna;

import java.util.List;

/**
 * Aplica al catálogo local el contenido completo recibido de SIGA. La operación
 * debe ser atómica: o se aplican todos los aulas y catálogos, o no se aplica
 * ninguno. Devuelve la cantidad de aulas procesadas.
 */
public interface CatalogoSigaPort {

    int aplicarAulasExternas(List<AulaExterna> aulas);
}
