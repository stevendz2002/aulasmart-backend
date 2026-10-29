package co.edu.uceva.aulasmart.aulas.domain.exception;

public class AulaNoEncontradaException extends RuntimeException {
    public AulaNoEncontradaException(Long idAula) {
        super("No existe un aula activa con el identificador " + idAula + ".");
    }
}
