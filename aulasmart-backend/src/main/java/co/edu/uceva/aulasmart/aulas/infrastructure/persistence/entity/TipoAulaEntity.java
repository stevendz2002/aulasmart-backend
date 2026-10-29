package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_aula")
public class TipoAulaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_aula")
    private Long id;
    @Column(name = "codigo_siga", nullable = false, unique = true)
    private Long codigoSiga;
    @Column(nullable = false)
    private String nombre;

    protected TipoAulaEntity() {
    }

    public static TipoAulaEntity crear(Long codigoSiga, String nombre) {
        var entity = new TipoAulaEntity();
        entity.codigoSiga = codigoSiga;
        entity.nombre = nombre;
        return entity;
    }

    public void actualizarNombre(String nombre) {
        this.nombre = nombre;
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
}
