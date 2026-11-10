package co.edu.uceva.aulasmart.aulabot.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Contexto estructurado mínimo de una conversación: la consulta vigente, la
 * aclaración pendiente y la franja parcial ya aportada. No guarda historial
 * completo ni resultados de disponibilidad.
 */
public record EstadoConversacionAulaBot(ConsultaAulaBot consultaVigente, String aclaracionPendiente,
        FranjaAulaBot franjaPendiente, Instant actualizadoEn) {
    public EstadoConversacionAulaBot {
        if (aclaracionPendiente != null && aclaracionPendiente.isBlank()) {
            aclaracionPendiente = null;
        }
        Objects.requireNonNull(actualizadoEn);
    }

    public static EstadoConversacionAulaBot inicial(Instant ahora) {
        return new EstadoConversacionAulaBot(null, null, null, Objects.requireNonNull(ahora));
    }

    public boolean vacia() {
        return consultaVigente == null && aclaracionPendiente == null && franjaPendiente == null;
    }
}
