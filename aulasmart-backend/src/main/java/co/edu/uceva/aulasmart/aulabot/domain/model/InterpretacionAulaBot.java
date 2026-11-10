package co.edu.uceva.aulasmart.aulabot.domain.model;

/**
 * Resultado de interpretar un mensaje: o hay una consulta completa o se deben
 * conservar los parámetros entendidos y aclarar lo que falta. Los filtros y la
 * franja parcial se combinan con el contexto vigente en el backend.
 */
public record InterpretacionAulaBot(ConsultaAulaBot consulta, FiltroAulaBot parcial, FranjaAulaBot franja, String aclaracion) {
    public InterpretacionAulaBot {
        if ((consulta == null) == (aclaracion == null || aclaracion.isBlank())) {
            throw new IllegalArgumentException(
                    "La interpretación debe contener una consulta completa o una aclaración, no ambas.");
        }
        if (consulta != null) {
            parcial = null;
            franja = null;
        }
    }

    public static InterpretacionAulaBot completa(ConsultaAulaBot consulta) {
        return new InterpretacionAulaBot(consulta, null, null, null);
    }

    public static InterpretacionAulaBot requiereAclaracion(String aclaracion) {
        return new InterpretacionAulaBot(null, null, null, aclaracion);
    }

    public static InterpretacionAulaBot requiereAclaracion(String aclaracion, FiltroAulaBot parcial) {
        return new InterpretacionAulaBot(null, parcial, null, aclaracion);
    }

    public static InterpretacionAulaBot requiereAclaracion(String aclaracion, FiltroAulaBot parcial, FranjaAulaBot franja) {
        return new InterpretacionAulaBot(null, parcial, franja, aclaracion);
    }

    public boolean estaCompleta() {
        return consulta != null;
    }
}
