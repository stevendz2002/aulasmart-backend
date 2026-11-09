package co.edu.uceva.aulasmart.users.presentation.dto;

import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;

import java.time.Instant;


/**
 * Perfil de un usuario del directorio para la SPA: sin claves ni hashes.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        boolean activo,
        String rol,
        Instant creadoEn
) {
    public static UsuarioResponse desde(PerfilUsuario perfil) {
        return new UsuarioResponse(
                perfil.id(),
                perfil.nombre(),
                perfil.correo(),
                perfil.activo(),
                perfil.rol(),
                perfil.creadoEn()
        );
    }
}
