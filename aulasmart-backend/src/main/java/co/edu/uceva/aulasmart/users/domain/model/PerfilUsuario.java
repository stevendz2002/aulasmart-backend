package co.edu.uceva.aulasmart.users.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Perfil de un usuario del sistema: el administrador lo consulta y crea
 * cuentas sobre este modelo de dominio. No expone la clave ni su hash.
 */
public record PerfilUsuario(
        Long id,
        String nombre,
        String correo,
        boolean activo,
        List<String> roles,
        Instant creadoEn
) {
}
