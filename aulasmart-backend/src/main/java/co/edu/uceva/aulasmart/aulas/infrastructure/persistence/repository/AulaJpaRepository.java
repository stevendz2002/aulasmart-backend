package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.AulaEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AulaJpaRepository extends JpaRepository<AulaEntity, Long> {
    @EntityGraph(attributePaths = {"edificio", "tipoAula", "dependencia"})
    @Query("""
            SELECT a FROM AulaEntity a
            WHERE a.activa = true
              AND (:idEdificio IS NULL OR a.edificio.id = :idEdificio)
              AND (:idTipoAula IS NULL OR a.tipoAula.id = :idTipoAula)
              AND (:idDependencia IS NULL OR a.dependencia.id = :idDependencia)
              AND (:capacidadMinima IS NULL OR a.capacidad >= :capacidadMinima)
            ORDER BY a.nombre
            """)
    List<AulaEntity> buscarActivas(
            @Param("idEdificio") Long idEdificio,
            @Param("idTipoAula") Long idTipoAula,
            @Param("idDependencia") Long idDependencia,
            @Param("capacidadMinima") Integer capacidadMinima
    );

    @EntityGraph(attributePaths = {"edificio", "tipoAula", "dependencia"})
    Optional<AulaEntity> findByIdAndActivaTrue(Long id);

    Optional<AulaEntity> findByCodigoSiga(Long codigoSiga);
}
