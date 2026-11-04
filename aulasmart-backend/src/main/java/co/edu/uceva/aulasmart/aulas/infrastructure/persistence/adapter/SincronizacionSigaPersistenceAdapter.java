package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository.SincronizacionSigaJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SincronizacionSigaPersistenceAdapter implements SincronizacionSigaPort {

    private final SincronizacionSigaJpaRepository repository;

    public SincronizacionSigaPersistenceAdapter(SincronizacionSigaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean catalogoAulasVigente(Instant limiteActualizacion) {
        return repository.existsByRecursoAndEstadoAndFinalizadaEnGreaterThanEqual(
                "AULAS", "EXITOSA", limiteActualizacion
        );
    }

    @Override
    public boolean eventosVigente(Instant limiteActualizacion) {
        return repository.existsByRecursoAndEstadoAndFinalizadaEnGreaterThanEqual(
                "EVENTOS", "EXITOSA", limiteActualizacion
        );
    }
}
