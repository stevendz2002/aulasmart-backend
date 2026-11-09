package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.aulas.domain.model.OcupacionAula;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.repository.SolicitudAulaJpaRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SolicitudAprobadaPersistenceAdapter implements SolicitudAprobadaPort {
    @Override
    public List<OcupacionAula> buscarOcupacion(Long aulaId, Intervalo intervalo) {
        return repository.buscarOcupacion(aulaId, intervalo.inicio(), intervalo.fin()).stream()
                .map(s -> new OcupacionAula(
                        aulaId, "Solicitud aprobada en AulaSmart", s.getFechaInicio(), s.getFechaFin())).toList();
    }
    private final SolicitudAulaJpaRepository repository;

    public SolicitudAprobadaPersistenceAdapter(SolicitudAulaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<Long> buscarAulasConSolicitudesAprobadas(Intervalo intervalo) {
        return repository.buscarAulasConSolicitudesAprobadas(intervalo.inicio(), intervalo.fin());
    }
}
