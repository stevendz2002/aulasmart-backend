package co.edu.uceva.aulasmart.users.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol")
public class RolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Short id;
    @Column(nullable = false, unique = true)
    private String nombre;

    protected RolEntity() {
    }

    public String getNombre() {
        return nombre;
    }
}
