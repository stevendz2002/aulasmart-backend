package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.adapter;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository.HorarioAcademicoJpaRepository;

import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class HorarioAcademicoPersistenceAdapter implements HorarioAcademicoPort {
    private final HorarioAcademicoJpaRepository repository;

    public HorarioAcademicoPersistenceAdapter(HorarioAcademicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<Long> buscarAulasOcupadas(Intervalo intervalo) {
        return repository.buscarAulasOcupadas(intervalo.inicio(), intervalo.fin());
    }
}
