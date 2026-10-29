package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.SincronizacionSigaEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;

public interface SincronizacionSigaJpaRepository extends JpaRepository<SincronizacionSigaEntity, Long> {
    boolean existsByRecursoAndEstadoAndFinalizadaEnGreaterThanEqual(
            String recurso,
            String estado,
            Instant limiteActualizacion
    );

    @Query("""
            SELECT COUNT(s) > 0 FROM SincronizacionSigaEntity s
            WHERE s.recurso = 'EVENTOS' AND s.estado = 'EXITOSA'
              AND s.finalizadaEn >= :limiteActualizacion
              AND s.ventanaInicio <= :fechaInicio
              AND s.ventanaFin >= :fechaFin
            """)
    boolean existeCoberturaEventosVigente(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("limiteActualizacion") Instant limiteActualizacion
    );
}
