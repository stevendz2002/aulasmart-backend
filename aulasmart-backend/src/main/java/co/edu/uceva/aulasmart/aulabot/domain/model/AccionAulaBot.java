package co.edu.uceva.aulasmart.aulabot.domain.model;

/** Acciones estructuradas de los chips: se ejecutan sin llamar al modelo de lenguaje. */
public enum AccionAulaBot {
    BUSCAR_AULA,
    CONSULTAR_DISPONIBILIDAD,
    CAMBIAR_BLOQUE,
    CAMBIAR_HORARIO,
    VER_OPCIONES;

    public static AccionAulaBot desde(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Acción de AulaBot no reconocida.");
        }
    }
}
