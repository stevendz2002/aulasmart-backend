package co.edu.uceva.aulasmart.users.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<UsuarioEntity> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    /** Directorio ordenado por fecha de creación descendente (roles cargados). */
    @EntityGraph(attributePaths = "roles")
    List<UsuarioEntity> findAllByOrderByCreadoEnDesc();
}
