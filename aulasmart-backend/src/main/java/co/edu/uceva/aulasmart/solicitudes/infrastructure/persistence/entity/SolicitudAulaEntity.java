package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "solicitud_aula")
public class SolicitudAulaEntity {
    @Version
    private Long version;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;
    @Column(name = "id_usuario", nullable = false)
    private Long docenteId;
    @Column(name = "id_aula", nullable = false)
    private Long aulaId;
    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;
    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;
    @Column(nullable = false)
    private String estado;
    @Column(nullable = false)
    private String motivo;
    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;
    @Column(name = "revisada_por")
    private Long revisadaPorId;
    @Column(name = "revisada_en")
    private Instant revisadaEn;

    protected SolicitudAulaEntity() {
    }

    /**
     * Crea una solicitud nueva en estado pendiente, sin datos de revisión.
     * El docente y el aula se registran por identificador; el estado PENDIENTE
     * también lo decide el caso de uso y llega por argumento.
     */
    public static SolicitudAulaEntity nueva(
            Long docenteId,
            Long aulaId,
            Instant fechaInicio,
            Instant fechaFin,
            String estado,
            String motivo,
            Instant creadaEn
    ) {
        var entity = new SolicitudAulaEntity();
        entity.docenteId = docenteId;
        entity.aulaId = aulaId;
        entity.fechaInicio = fechaInicio;
        entity.fechaFin = fechaFin;
        entity.estado = estado;
        entity.motivo = motivo;
        entity.creadaEn = creadaEn;
        return entity;
    }

    /**
     * Aplica la decisión de revisión sobre la solicitud existente: cambia el
     * estado y deja la auditoría de quién revisó y cuándo.
     */
    public void revisar(String estado, Long revisadaPorId, Instant revisadaEn) {
        this.estado = estado;
        this.revisadaPorId = revisadaPorId;
        this.revisadaEn = revisadaEn;
    }

    public Long getId() {
        return id;
    }

    public Long getDocenteId() {
        return docenteId;
    }

    public Long getAulaId() {
        return aulaId;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Long getRevisadaPorId() {
        return revisadaPorId;
    }

    public Instant getRevisadaEn() {
        return revisadaEn;
    }
}
