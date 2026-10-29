package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class SincronizacionSigaPersistenceAdapter implements SincronizacionSigaPort {
    private static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Bogota");

    private final SincronizacionSigaJpaRepository repository;

    public SincronizacionSigaPersistenceAdapter(SincronizacionSigaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean datosCompletosYVigentes(Intervalo intervalo, Instant limiteActualizacion) {
        boolean aulasVigentes = repository.existsByRecursoAndEstadoAndFinalizadaEnGreaterThanEqual(
                "AULAS", "EXITOSA", limiteActualizacion
        );
        if (!aulasVigentes) {
            return false;
        }

        LocalDate fechaInicio = LocalDate.ofInstant(intervalo.inicio(), ZONA_INSTITUCIONAL);
        LocalDate fechaFin = LocalDate.ofInstant(intervalo.fin(), ZONA_INSTITUCIONAL);
        return repository.existeCoberturaEventosVigente(fechaInicio, fechaFin, limiteActualizacion);
    }
}
