package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.DependenciaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DependenciaJpaRepository extends JpaRepository<DependenciaEntity, Long> {
    Optional<DependenciaEntity> findByCodigoSiga(Long codigoSiga);
}
