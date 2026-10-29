package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_aula")
public class TipoAulaEntity {
    @Id
    @Column(name = "id_tipo_aula")
    private Long id;
    @Column(name = "codigo_siga", nullable = false, unique = true)
    private Long codigoSiga;
    @Column(nullable = false)
    private String nombre;

    protected TipoAulaEntity() {
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
