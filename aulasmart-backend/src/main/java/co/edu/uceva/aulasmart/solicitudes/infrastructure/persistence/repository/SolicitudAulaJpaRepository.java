package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.entity.SolicitudAulaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudAulaJpaRepository extends JpaRepository<SolicitudAulaEntity, Long> {
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SolicitudAulaEntity s SET s.estado = 'RECHAZADA', s.revisadaEn = :ahora, s.version = s.version + 1 WHERE s.estado = 'PENDIENTE' AND s.fechaInicio <= :ahora")
    int rechazarVencidas(@Param("ahora") Instant ahora);
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        DELETE FROM solicitud_aula WHERE id_solicitud IN (
          SELECT id_solicitud FROM (
            SELECT id_solicitud, row_number() OVER (
              PARTITION BY id_usuario ORDER BY creada_en DESC, id_solicitud DESC) AS posicion
            FROM solicitud_aula WHERE estado = 'RECHAZADA' OR fecha_fin <= :ahora
          ) historial WHERE posicion > 50)
        """, nativeQuery = true)
    int depurarHistorial(@Param("ahora") Instant ahora);

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
