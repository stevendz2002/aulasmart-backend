package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "aula")
public class AulaEntity {
    @Id
    @Column(name = "id_aula")
    private Long id;
    @Column(name = "codigo_siga", nullable = false, unique = true)
    private Long codigoSiga;
    @Column(nullable = false)
    private String nombre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_edificio", nullable = false)
    private EdificioEntity edificio;
    @Column(nullable = false)
    private int capacidad;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_aula", nullable = false)
    private TipoAulaEntity tipoAula;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dependencia", nullable = false)
    private DependenciaEntity dependencia;
    @Column(nullable = false)
    private boolean activa;

    protected AulaEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getCodigoSiga() {
        return codigoSiga;
    }

    public String getNombre() {
        return nombre;
    }

    public EdificioEntity getEdificio() {
        return edificio;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public TipoAulaEntity getTipoAula() {
        return tipoAula;
    }

    public DependenciaEntity getDependencia() {
        return dependencia;
    }

    public boolean isActiva() {
        return activa;
    }
}
