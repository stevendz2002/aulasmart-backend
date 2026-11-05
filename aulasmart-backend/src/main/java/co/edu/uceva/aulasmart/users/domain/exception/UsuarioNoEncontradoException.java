package co.edu.uceva.aulasmart.users.domain.exception;

/**
 * La cuenta solicitada no existe en el directorio de usuarios.
 */
public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(Long idUsuario) {
        super("No existe una cuenta de usuario con el identificador " + idUsuario + ".");
    }
}
