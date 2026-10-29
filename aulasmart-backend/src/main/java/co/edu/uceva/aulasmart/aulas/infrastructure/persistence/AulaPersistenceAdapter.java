package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Aula;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaFiltro;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AulaPersistenceAdapter implements AulaConsultaPort {
    private final AulaJpaRepository repository;

    public AulaPersistenceAdapter(AulaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Aula> buscarActivas(AulaFiltro filtro) {
        return repository.buscarActivas(
                        filtro.idEdificio(),
                        filtro.idTipoAula(),
                        filtro.idDependencia(),
                        filtro.capacidadMinima()
                ).stream()
                .map(AulaMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Aula> buscarActivaPorId(Long idAula) {
        return repository.findByIdAndActivaTrue(idAula).map(AulaMapper::toDomain);
    }
}
