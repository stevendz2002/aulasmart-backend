package co.edu.uceva.aulasmart.users.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;



@Entity
@Table(name = "usuario")
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;
    @Column(name = "nombre", nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String correo;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private boolean activo;
    @Column(name = "creado_en")
    private Instant creadoEn;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private RolEntity rol;

    protected UsuarioEntity() {
    }

    /**
     * Cuenta activa en su registro: el administrador crea cuentas ya
     * habilitadas. La fecha de creación se fija en el momento de persistir.
     */
    public static UsuarioEntity registrar(String nombre, String correo, String claveCodificada, Instant creadoEn) {
        var entidad = new UsuarioEntity();
        entidad.nombre = nombre;
        entidad.correo = correo;
        entidad.passwordHash = claveCodificada;
        entidad.activo = true;
        entidad.creadoEn = creadoEn;
        return entidad;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public RolEntity getRol() {
        return rol;
    }

    public void asignarRol(RolEntity rol) {
        this.rol = java.util.Objects.requireNonNull(rol, "El rol es obligatorio.");
    }

    /** Edición del perfil por el administrador (nombre de la cuenta). */
    public void actualizarNombre(String nombre) {
        this.nombre = nombre;
    }

    /** Activa o desactiva la cuenta (el administrador gestiona el acceso). */
    public void actualizarActivo(boolean activo) {
        this.activo = activo;
    }
}
