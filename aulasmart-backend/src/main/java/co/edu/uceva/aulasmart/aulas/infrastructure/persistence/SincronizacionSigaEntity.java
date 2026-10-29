package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sincronizacion_siga")
public class SincronizacionSigaEntity {
    @Id
    @Column(name = "id_sincronizacion")
    private Long id;
    @Column(nullable = false)
    private String recurso;
    @Column(nullable = false)
    private String estado;
    @Column(name = "finalizada_en")
    private Instant finalizadaEn;
    @Column(name = "ventana_inicio")
    private LocalDate ventanaInicio;
    @Column(name = "ventana_fin")
    private LocalDate ventanaFin;

    protected SincronizacionSigaEntity() {
    }
}
