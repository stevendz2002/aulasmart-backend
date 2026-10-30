package co.edu.uceva.aulasmart.users.application.port;

import java.util.Optional;
import java.util.Set;

/**
 * Consulta de usuarios por correo para resolver el principal autenticado:
 * entrega su identificador y sus roles sin exponer la relación de credenciales.
 */
public interface ConsultaUsuariosPort {

    Optional<UsuarioAutenticado> buscarPorCorreo(String correo);

    record UsuarioAutenticado(Long id, Set<String> roles) {
    }
}
