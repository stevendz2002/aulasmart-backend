package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.entity.SolicitudAulaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudAulaJpaRepository extends JpaRepository<SolicitudAulaEntity, Long> {
    @Query("SELECT s FROM SolicitudAulaEntity s WHERE s.aulaId = :aula AND s.estado = 'APROBADA' AND s.fechaInicio < :fin AND s.fechaFin > :inicio ORDER BY s.fechaInicio")
    List<SolicitudAulaEntity> buscarOcupacion(@Param("aula") Long aula, @Param("inicio") Instant inicio, @Param("fin") Instant fin);
    @Query("""
            SELECT DISTINCT s.aulaId FROM SolicitudAulaEntity s
            WHERE s.estado = 'APROBADA'
              AND s.fechaInicio < :fin AND s.fechaFin > :inicio
            """)
    Set<Long> buscarAulasConSolicitudesAprobadas(
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin
    );

    List<SolicitudAulaEntity> findByDocenteIdOrderByCreadaEnDesc(Long docenteId);

    List<SolicitudAulaEntity> findAllByOrderByCreadaEnDesc();

    List<SolicitudAulaEntity> findByEstadoOrderByCreadaEnDesc(String estado);
}
