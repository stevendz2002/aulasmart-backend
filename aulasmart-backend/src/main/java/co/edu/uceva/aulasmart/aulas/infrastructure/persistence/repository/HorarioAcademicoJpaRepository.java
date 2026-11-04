package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.HorarioAcademicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface HorarioAcademicoJpaRepository extends JpaRepository<HorarioAcademicoEntity, Long> {

    /**
     * Aulas con una franja de ocupación que se solapa con el intervalo
     * consultado: la tabla usa un índice GiST sobre
     * `tstzrange(fecha_inicio, fecha_fin, '[)')` y esta condición replica esa
     * semántica de solapamiento en la consulta JPQL.
     */
    @Query("""
            SELECT DISTINCT h.aulaId FROM HorarioAcademicoEntity h
            WHERE h.fechaInicio < :fin AND h.fechaFin > :inicio
            """)
    Set<Long> buscarAulasConEventos(
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin
    );

    @Query("""
            SELECT h FROM HorarioAcademicoEntity h
            WHERE h.aulaId = :idAula
              AND h.fechaInicio < :fin AND h.fechaFin > :inicio
            ORDER BY h.fechaInicio
            """)
    List<HorarioAcademicoEntity> buscarOcupacionDeAula(
            @Param("idAula") Long idAula,
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin
    );

    /**
     * Descarte previo a la inserción de una ventana sincronizada de eventos
     * SIGA: la sincronización de eventos reemplaza todo lo conocido desde
     * `inicioVentana`, de modo que la tabla siempre refleja la última
     * corrida exitosa (sin filas huérfanas de ventanas anteriores).
     */
    @Modifying
    @Query("""
            DELETE FROM HorarioAcademicoEntity h
            WHERE h.fuente = 'SIGA'
              AND h.fechaInicio >= :inicioVentana
            """)
    int eliminarDesde(@Param("inicioVentana") Instant inicioVentana);
}
