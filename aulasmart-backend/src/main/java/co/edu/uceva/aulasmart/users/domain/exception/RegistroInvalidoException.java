package co.edu.uceva.aulasmart.users.domain.exception;

/**
 * Datos de registro incompletos o sin formato válido (nombre, correo o clave).
 */
public class RegistroInvalidoException extends RuntimeException {
    public RegistroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
