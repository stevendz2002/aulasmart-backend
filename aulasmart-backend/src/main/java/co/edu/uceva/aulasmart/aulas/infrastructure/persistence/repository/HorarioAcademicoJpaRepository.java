package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.HorarioAcademicoEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Set;

public interface HorarioAcademicoJpaRepository extends JpaRepository<HorarioAcademicoEntity, Long> {
    @Query("""
            SELECT DISTINCT h.aulaId FROM HorarioAcademicoEntity h
            WHERE h.fechaInicio < :fin AND h.fechaFin > :inicio
            """)
    Set<Long> buscarAulasOcupadas(@Param("inicio") Instant inicio, @Param("fin") Instant fin);
}
