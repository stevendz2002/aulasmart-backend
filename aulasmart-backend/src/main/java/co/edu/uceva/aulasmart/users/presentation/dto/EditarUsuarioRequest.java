package co.edu.uceva.aulasmart.users.presentation.dto;

import java.util.List;

/**
 * Cambios sobre una cuenta existente. Cada campo ausente (null) se conserva
 * tal cual: editar nombre y roles, o desactivar/activar la cuenta. Solo el
 * rol ADMINISTRADOR puede enviar este cuerpo.
 */
public record EditarUsuarioRequest(
        String nombre,
        List<String> roles,
        Boolean activo
) {
}
