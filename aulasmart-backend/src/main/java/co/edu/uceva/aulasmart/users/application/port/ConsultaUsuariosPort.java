package co.edu.uceva.aulasmart.users.application.port;

import java.util.Optional;


/**
 * Consulta de usuarios por correo para resolver el principal autenticado:
 * entrega su identificador, nombre real de la cuenta y sus roles sin exponer
 * la relación de credenciales.
 */
public interface ConsultaUsuariosPort {

    Optional<UsuarioAutenticado> buscarPorCorreo(String correo);

    record UsuarioAutenticado(Long id, String nombre, String rol) {
    }
}
