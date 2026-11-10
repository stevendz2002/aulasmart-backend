package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.adapter;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.repository.SolicitudAulaJpaRepository;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
@Component
public class CicloSolicitudes {
    private final SolicitudAulaJpaRepository repository;
    private final Clock clock;
    public CicloSolicitudes(SolicitudAulaJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }
    @Scheduled(fixedDelayString = "${aulasmart.solicitudes.limpieza-ms:1000}")
    @Transactional
    public void actualizar() {
        var ahora = clock.instant();
        repository.rechazarVencidas(ahora);
        repository.depurarHistorial(ahora);
    }
}
