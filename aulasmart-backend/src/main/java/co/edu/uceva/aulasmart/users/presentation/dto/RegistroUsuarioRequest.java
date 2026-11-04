package co.edu.uceva.aulasmart.users.presentation.dto;

import java.util.List;

/**
 * Datos de registro de una nueva cuenta. Solo el rol ADMINISTRADOR puede
 * enviar este cuerpo (regla de acceso en la configuración de seguridad).
 */
public record RegistroUsuarioRequest(
        String nombre,
        String correo,
        String clave,
        List<String> roles
) {
}
