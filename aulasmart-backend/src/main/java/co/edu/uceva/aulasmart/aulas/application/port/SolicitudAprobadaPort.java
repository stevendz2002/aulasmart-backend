package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;

import java.util.Set;

public interface SolicitudAprobadaPort {
    Set<Long> buscarAulasConSolicitudesAprobadas(Intervalo intervalo);
}
