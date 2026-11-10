package co.edu.uceva.aulasmart.aulabot.domain.model;

public class AulaBotNoDisponibleException extends RuntimeException {
    public AulaBotNoDisponibleException() {
        super("AulaBot no está disponible o alcanzó su cuota gratuita. Puedes continuar en Consulta de aulas.");
    }
}
