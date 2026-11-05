package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.adapter;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.repository.SolicitudAulaJpaRepository;

import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SolicitudAprobadaPersistenceAdapter implements SolicitudAprobadaPort {
    private final SolicitudAulaJpaRepository repository;

    public SolicitudAprobadaPersistenceAdapter(SolicitudAulaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<Long> buscarAulasConSolicitudesAprobadas(Intervalo intervalo) {
        return repository.buscarAulasConSolicitudesAprobadas(intervalo.inicio(), intervalo.fin());
    }
}
