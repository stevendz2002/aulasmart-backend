package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.aulas.domain.model.OcupacionAula;
import java.util.List;
import java.util.Set;

public interface SolicitudAprobadaPort {
    List<OcupacionAula> buscarOcupacion(Long aulaId, Intervalo intervalo);
    Set<Long> buscarAulasConSolicitudesAprobadas(Intervalo intervalo);
}
