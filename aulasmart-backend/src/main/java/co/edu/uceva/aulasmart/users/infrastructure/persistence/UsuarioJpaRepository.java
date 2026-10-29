package co.edu.uceva.aulasmart.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<UsuarioEntity> findByCorreoIgnoreCase(String correo);
}
