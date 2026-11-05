package co.edu.uceva.aulasmart.users.domain.exception;

/**
 * El correo institucional ya tiene una cuenta asociada: la creación se
 * rechaza (válido para manterner la unicidad del correo en el sistema).
 */
public class CorreoYaRegistradoException extends RuntimeException {
    public CorreoYaRegistradoException(String correo) {
        super("Ya existe una cuenta con el correo " + correo + ".");
    }
}
