package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoAulaJpaRepository extends JpaRepository<TipoAulaEntity, Long> {
    Optional<TipoAulaEntity> findByCodigoSiga(Long codigoSiga);
}
