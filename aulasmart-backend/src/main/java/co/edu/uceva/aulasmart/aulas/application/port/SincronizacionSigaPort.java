package co.edu.uceva.aulasmart.aulas.application.port;

import java.time.Instant;

/**
 * Condición de la puerta de calidad de la disponibilidad: el catálogo de
 * aulas sincronizado desde SIGA no debe ser más viejo que la antigüedad
 * máxima configurada. Las ocupaciones provienen exclusivamente de las
 * reservas aprobadas dentro de AulaSmart.
 */
public interface SincronizacionSigaPort {
    boolean catalogoAulasVigente(Instant limiteActualizacion);
}
