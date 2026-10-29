package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.repository;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.entity.SolicitudAulaEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Set;

public interface SolicitudAulaJpaRepository extends JpaRepository<SolicitudAulaEntity, Long> {
    @Query("""
            SELECT DISTINCT s.aulaId FROM SolicitudAulaEntity s
            WHERE s.estado = 'APROBADA'
              AND s.fechaInicio < :fin AND s.fechaFin > :inicio
            """)
    Set<Long> buscarAulasConSolicitudesAprobadas(
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin
    );
}
