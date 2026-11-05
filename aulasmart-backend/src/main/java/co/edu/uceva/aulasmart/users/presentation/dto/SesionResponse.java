package co.edu.uceva.aulasmart.users.presentation.dto;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * Perfil de sesion del usuario autenticado. El nombre es el real de la
 * cuenta (tabla usuario) y los roles se derivan de las autoridades cargadas
 * por el UserDetailsService en la peticion actual, sin volver a consultar la
 * relacion de credenciales.
 */
public record SesionResponse(
        String correo,
        String nombre,
        List<String> roles
) {

    public static SesionResponse desde(
            Authentication authentication,
            String nombreDeLaCuenta
    ) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(autoridad -> autoridad.replaceFirst("^ROLE_", ""))
                .toList();
        return new SesionResponse(authentication.getName(), nombreDeLaCuenta, roles);
    }
}
