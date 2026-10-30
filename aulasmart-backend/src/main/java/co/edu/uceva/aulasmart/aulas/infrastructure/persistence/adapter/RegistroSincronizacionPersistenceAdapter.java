package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.adapter;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository.SincronizacionSigaJpaRepository;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.SincronizacionSigaEntity;

import co.edu.uceva.aulasmart.aulas.application.port.RegistroSincronizacionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class RegistroSincronizacionPersistenceAdapter implements RegistroSincronizacionPort {

    private final SincronizacionSigaJpaRepository repository;
    private final Clock clock;

    public RegistroSincronizacionPersistenceAdapter(SincronizacionSigaJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Long registrarInicio(String recurso) {
        var registro = SincronizacionSigaEntity.crear(recurso, clock.instant());
        return repository.save(registro).getId();
    }

    @Override
    @Transactional
    public void registrarFin(Long idSincronizacion, String estado, int registrosProcesados, String detalleError) {
        repository.findById(idSincronizacion).ifPresent(registro ->
                registro.finalizar(estado, clock.instant(), registrosProcesados, detalleError));
    }
}
