package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "aula")
public class AulaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public static AulaEntity crear(
            Long codigoSiga,
            String nombre,
            EdificioEntity edificio,
            int capacidad,
            TipoAulaEntity tipoAula,
            DependenciaEntity dependencia,
            boolean activa
    ) {
        var entity = new AulaEntity();
        entity.codigoSiga = codigoSiga;
        entity.nombre = nombre;
        entity.edificio = edificio;
        entity.capacidad = capacidad;
        entity.tipoAula = tipoAula;
        entity.dependencia = dependencia;
        entity.activa = activa;
        return entity;
    }

    /**
     * Actualiza los atributos observables en SIGA. `activa` es un atributo local
     * de AulaSmart y deliberadamente no se modifica en la sincronización.
     */
    public void actualizar(
            String nombre,
            int capacidad,
            EdificioEntity edificio,
            TipoAulaEntity tipoAula,
            DependenciaEntity dependencia
    ) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.edificio = edificio;
        this.tipoAula = tipoAula;
        this.dependencia = dependencia;
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
