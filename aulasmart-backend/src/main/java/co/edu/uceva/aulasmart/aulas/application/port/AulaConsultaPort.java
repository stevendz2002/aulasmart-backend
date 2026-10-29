package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.Aula;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaFiltro;

import java.util.List;
import java.util.Optional;

public interface AulaConsultaPort {
    List<Aula> buscarActivas(AulaFiltro filtro);

    Optional<Aula> buscarActivaPorId(Long idAula);
}
