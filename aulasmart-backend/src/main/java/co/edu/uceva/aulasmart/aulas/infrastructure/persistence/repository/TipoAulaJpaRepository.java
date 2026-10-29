package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.TipoAulaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoAulaJpaRepository extends JpaRepository<TipoAulaEntity, Long> {
    Optional<TipoAulaEntity> findByCodigoSiga(Long codigoSiga);
}
