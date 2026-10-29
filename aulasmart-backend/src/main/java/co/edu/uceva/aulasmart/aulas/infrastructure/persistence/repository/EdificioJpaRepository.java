package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.EdificioEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EdificioJpaRepository extends JpaRepository<EdificioEntity, Long> {
    Optional<EdificioEntity> findByCodigoSiga(Long codigoSiga);
}
