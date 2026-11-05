package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Fila de ocupación académica de un aula (tabla `horario_academico` del
 * esquema V1): una franja tomada por una clase u evento sincronizado desde
 * SIGA. `fuente` distingue la procedencia (SIGA u otra futura) y
 * `id_externo` rastrea la ocurrencia en el sistema de origen.
 */
@Entity
@Table(name = "horario_academico")
public class HorarioAcademicoEntity {

    public static final String FUENTE_SIGA = "SIGA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long id;

    @Column(name = "id_aula", nullable = false)
    private Long aulaId;

    @Column(name = "id_externo")
    private String idExterno;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;

    @Column(nullable = false)
    private String fuente;

    @Column(name = "detalles")
    private String detalles;

    protected HorarioAcademicoEntity() {
    }

    public static HorarioAcademicoEntity crear(
            Long aulaId,
            String idExterno,
            Instant fechaInicio,
            Instant fechaFin,
            String detalles
    ) {
        var entidad = new HorarioAcademicoEntity();
        entidad.aulaId = aulaId;
        entidad.idExterno = idExterno;
        entidad.fechaInicio = fechaInicio;
        entidad.fechaFin = fechaFin;
        entidad.fuente = FUENTE_SIGA;
        entidad.detalles = detalles;
        return entidad;
    }

    public Long getId() {
        return id;
    }

    public Long getAulaId() {
        return aulaId;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public String getFuente() {
        return fuente;
    }

    public String getDetalles() {
        return detalles;
    }
}
