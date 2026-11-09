package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import java.time.Instant;

/**
 * Condición de la puerta de calidad de la disponibilidad: los recursos
 * sincronizados desde SIGA (catálogo de aulas y eventos de ocupación) no
 * deben ser más viejos que la antigüedad máxima configurada. Las ocupaciones
 * provienen de los eventos SIGA unidos con las reservas aprobadas.
 */
public interface SincronizacionSigaPort {
    boolean eventosCubren(Instant limiteActualizacion, Intervalo intervalo);
    boolean catalogoAulasVigente(Instant limiteActualizacion);

    boolean eventosVigente(Instant limiteActualizacion);
}
