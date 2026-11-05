package co.edu.uceva.aulasmart.users.presentation.dto;

import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;

import java.time.Instant;
import java.util.List;

/**
 * Perfil de un usuario del directorio para la SPA: sin claves ni hashes.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        boolean activo,
        List<String> roles,
        Instant creadoEn
) {
    public static UsuarioResponse desde(PerfilUsuario perfil) {
        return new UsuarioResponse(
                perfil.id(),
                perfil.nombre(),
                perfil.correo(),
                perfil.activo(),
                perfil.roles(),
                perfil.creadoEn()
        );
    }
}
