package co.edu.uceva.aulasmart.users.presentation.dto;



/**
 * Datos de registro de una nueva cuenta. Solo el rol ADMINISTRADOR puede
 * enviar este cuerpo (regla de acceso en la configuración de seguridad).
 */
public record RegistroUsuarioRequest(
        String nombre,
        String correo,
        String clave,
        String rol
) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rechazarCampoDesconocido(String nombre, Object valor) {
        throw new IllegalArgumentException("Campo no admitido: " + nombre + ". Use rol singular.");
    }
}
