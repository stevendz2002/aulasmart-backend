package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;

import java.time.Instant;

public interface SincronizacionSigaPort {
    boolean datosCompletosYVigentes(Intervalo intervalo, Instant limiteActualizacion);
}
