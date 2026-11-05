package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.SincronizacionSigaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SincronizacionSigaJpaRepository extends JpaRepository<SincronizacionSigaEntity, Long> {
    boolean existsByRecursoAndEstadoAndFinalizadaEnGreaterThanEqual(
            String recurso,
            String estado,
            Instant limiteActualizacion
    );
}
