package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.SincronizacionSigaEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SincronizacionSigaJpaRepository extends JpaRepository<SincronizacionSigaEntity, Long> {
    Optional<SincronizacionSigaEntity> findFirstByRecursoOrderByIdDesc(String recurso);
    boolean existsByRecursoAndEstadoAndFinalizadaEnGreaterThanEqual(
            String recurso,
            String estado,
            Instant limiteActualizacion
    );
}
