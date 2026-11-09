package co.edu.uceva.aulasmart.integrations.siga.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.AulaExterna;
import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import java.time.LocalDate;
import java.util.List;

/** Consultas externas requeridas por los casos de uso de sincronización. */
public interface ConsultaSigaPort {
    List<AulaExterna> listarAulas();
    List<EventoExterno> listarEventos(Long codigoAula, LocalDate inicio, LocalDate fin);
}
