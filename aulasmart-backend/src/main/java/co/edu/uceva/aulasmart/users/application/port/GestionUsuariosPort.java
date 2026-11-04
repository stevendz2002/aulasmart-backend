package co.edu.uceva.aulasmart.users.application.port;

import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;

import java.util.List;

/**
 * Directorio de usuarios: listado y creación de cuentas. Solamente la
 * configuración de seguridad del backend permite esta operacion al rol
 * ADMINISTRADOR (regla de acceso del documento de arquitectura).
 */
public interface GestionUsuariosPort {

    List<PerfilUsuario> listar();

    boolean existeCorreo(String correo);

    PerfilUsuario crear(String nombre, String correo, String claveCodificada, List<String> roles);
}
