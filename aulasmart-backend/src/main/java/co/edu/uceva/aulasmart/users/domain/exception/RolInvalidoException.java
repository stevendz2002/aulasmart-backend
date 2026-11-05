package co.edu.uceva.aulasmart.users.domain.exception;

/**
 * Rol de la cuenta inexistente en el catálogo de roles del sistema.
 */
public class RolInvalidoException extends RuntimeException {
    public RolInvalidoException(String rol) {
        super("El rol " + rol + " no pertenece al catálogo de roles del sistema.");
    }
}
