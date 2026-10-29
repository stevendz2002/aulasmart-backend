package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;

import java.util.Set;

public interface HorarioAcademicoPort {
    Set<Long> buscarAulasOcupadas(Intervalo intervalo);
}
