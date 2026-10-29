package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "edificio")
public class EdificioEntity {
    @Id
    @Column(name = "id_edificio")
    private Long id;
    @Column(name = "codigo_siga", nullable = false, unique = true)
    private Long codigoSiga;
    @Column(nullable = false)
    private String nombre;

    protected EdificioEntity() {
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
