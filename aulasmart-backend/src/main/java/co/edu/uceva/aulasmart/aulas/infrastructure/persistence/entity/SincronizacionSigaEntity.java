package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sincronizacion_siga")
public class SincronizacionSigaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sincronizacion")
    private Long id;
    @Column(nullable = false)
    private String recurso;
    @Column(name = "iniciada_en", nullable = false)
    private Instant iniciadaEn;
    @Column(name = "finalizada_en")
    private Instant finalizadaEn;
    @Column(nullable = false)
    private String estado;
    @Column(name = "registros_procesados", nullable = false)
    private int registrosProcesados;
    @Column(name = "ventana_inicio")
    private LocalDate ventanaInicio;
    @Column(name = "ventana_fin")
    private LocalDate ventanaFin;
    @Column(name = "detalle_error")
    private String detalleError;

    protected SincronizacionSigaEntity() {
    }

    /**
     * Crea el registro de control de una sincronización. El estado inicial es
     * `EN_CURSO` con `finalizada_en` nula; posteriormente {@link #finalizar}
     * actualiza el resultado en el mismo registro.
     */
    public static SincronizacionSigaEntity crear(String recurso, Instant iniciadaEn,
            LocalDate ventanaInicio, LocalDate ventanaFin) {
        var entity = new SincronizacionSigaEntity();
        entity.recurso = recurso;
        entity.estado = "EN_CURSO";
        entity.iniciadaEn = iniciadaEn;
        entity.registrosProcesados = 0;
        entity.ventanaInicio = ventanaInicio;
        entity.ventanaFin = ventanaFin;
        return entity;
    }

    public void finalizar(String estado, Instant finalizadaEn, int registrosProcesados, String detalleError) {
        this.estado = estado;
        this.finalizadaEn = finalizadaEn;
        this.registrosProcesados = registrosProcesados;
        this.detalleError = detalleError;
    }

    public Long getId() {
        return id;
    }

    public String getRecurso() {
        return recurso;
    }

    public Instant getIniciadaEn() {
        return iniciadaEn;
    }

    public Instant getFinalizadaEn() {
        return finalizadaEn;
    }

    public String getEstado() {
        return estado;
    }

    public int getRegistrosProcesados() {
        return registrosProcesados;
    }

    public LocalDate getVentanaInicio() {
        return ventanaInicio;
    }

    public LocalDate getVentanaFin() {
        return ventanaFin;
    }

    public String getDetalleError() {
        return detalleError;
    }
}
