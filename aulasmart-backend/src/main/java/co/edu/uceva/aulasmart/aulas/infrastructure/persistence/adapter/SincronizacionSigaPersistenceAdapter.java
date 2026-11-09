package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository.SincronizacionSigaJpaRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SincronizacionSigaPersistenceAdapter implements SincronizacionSigaPort {
    @Override
    public boolean eventosCubren(Instant limite, Intervalo intervalo) {
        var catalogo = repository.findFirstByRecursoOrderByIdDesc("AULAS");
        if (catalogo.isEmpty() || !"EXITOSA".equals(catalogo.get().getEstado())) {
            return false;
        }
        return repository.findFirstByRecursoOrderByIdDesc("EVENTOS")
                .filter(r -> "EXITOSA".equals(r.getEstado()))
                .filter(r -> !r.getIniciadaEn().isBefore(catalogo.get().getFinalizadaEn()))
                .map(r -> r.cubre(intervalo.inicio(), intervalo.fin(), limite)).orElse(false);
    }

    private final SincronizacionSigaJpaRepository repository;

    public SincronizacionSigaPersistenceAdapter(SincronizacionSigaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean catalogoAulasVigente(Instant limiteActualizacion) {
        return repository.existsByRecursoAndEstadoAndFinalizadaEnGreaterThanEqual(
                "AULAS", "EXITOSA", limiteActualizacion
        );
    }

    @Override
    public boolean eventosVigente(Instant limiteActualizacion) {
        return repository.existsByRecursoAndEstadoAndFinalizadaEnGreaterThanEqual(
                "EVENTOS", "EXITOSA", limiteActualizacion
        );
    }
}
