package co.edu.uceva.aulasmart.users.application.port;

import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;

import java.util.List;

/**
 * Directorio de usuarios: listado, creación y edición de cuentas.
 * Solamente la configuración de seguridad del backend permite estas
 * operaciones al rol ADMINISTRADOR (regla de acceso del documento de
 * arquitectura).
 */
public interface GestionUsuariosPort {

    List<PerfilUsuario> listar();

    boolean existeCorreo(String correo);

    PerfilUsuario crear(String nombre, String correo, String claveCodificada, String rol);

    /**
     * Edita una cuenta existente: los campos en null no se modifican.
     * Devuelve el perfil vigente tras el cambio.
     */
    PerfilUsuario editar(Long idUsuario, String nombre, String rol, Boolean activo);
}
