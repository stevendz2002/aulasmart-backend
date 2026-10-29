package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "solicitud_aula")
public class SolicitudAulaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;
    @Column(name = "id_aula", nullable = false)
    private Long aulaId;
    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;
    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;
    @Column(nullable = false)
    private String estado;

    protected SolicitudAulaEntity() {
    }
}
