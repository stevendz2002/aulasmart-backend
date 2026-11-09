package co.edu.uceva.aulasmart.users.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    @EntityGraph(attributePaths = "rol")
    Optional<UsuarioEntity> findByCorreoIgnoreCase(String correo);

    @org.springframework.data.jpa.repository.Query("""
            SELECT count(u) FROM UsuarioEntity u
            WHERE u.activo = true AND u.rol.nombre = 'ADMINISTRADOR'
              AND (:permitirPruebas = true OR lower(u.correo) NOT LIKE '%@prueba.aulasmart.local')
            """)
    long contarAdministradoresHabilitados(@org.springframework.data.repository.query.Param("permitirPruebas") boolean permitirPruebas);

    boolean existsByCorreoIgnoreCase(String correo);

    /** Directorio ordenado por fecha de creación descendente (rol cargado). */
    @EntityGraph(attributePaths = "rol")
    List<UsuarioEntity> findAllByOrderByCreadoEnDesc();
}
