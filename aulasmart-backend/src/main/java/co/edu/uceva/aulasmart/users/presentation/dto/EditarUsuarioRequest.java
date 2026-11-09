package co.edu.uceva.aulasmart.users.presentation.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Cambios sobre una cuenta existente. Cada campo ausente (null) se conserva
 * tal cual: editar nombre y rol, o desactivar/activar la cuenta. Solo el
 * rol ADMINISTRADOR puede enviar este cuerpo.
 */
public final class EditarUsuarioRequest {
    private String nombre;
    private String rol;
    private Boolean activo;

    public String nombre() { return nombre; }
    public String rol() { return rol; }
    public Boolean activo() { return activo; }

    @JsonSetter("nombre")
    public void setNombre(String nombre) { this.nombre = nombre; }

    @JsonSetter("activo")
    public void setActivo(Boolean activo) { this.activo = activo; }

    @JsonSetter("rol")
    public void setRol(String rol) {
        if (rol == null) throw new IllegalArgumentException("El rol no puede ser null.");
        this.rol = rol;
    }
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rechazarCampoDesconocido(String nombre, Object valor) {
        throw new IllegalArgumentException("Campo no admitido: " + nombre + ". Use rol singular.");
    }
}
