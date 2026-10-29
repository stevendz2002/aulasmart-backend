package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "horario_academico")
public class HorarioAcademicoEntity {
    @Id
    @Column(name = "id_horario")
    private Long id;
    @Column(name = "id_aula", nullable = false)
    private Long aulaId;
    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;
    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;

    protected HorarioAcademicoEntity() {
    }
}
