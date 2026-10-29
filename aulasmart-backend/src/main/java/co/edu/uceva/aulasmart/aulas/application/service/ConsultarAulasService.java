package co.edu.uceva.aulasmart.aulas.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.domain.exception.AulaNoEncontradaException;
import co.edu.uceva.aulasmart.aulas.domain.model.Aula;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaFiltro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsultarAulasService {
    private final AulaConsultaPort aulaConsultaPort;

    public ConsultarAulasService(AulaConsultaPort aulaConsultaPort) {
        this.aulaConsultaPort = aulaConsultaPort;
    }

    @Transactional(readOnly = true)
    public List<Aula> consultar(AulaFiltro filtro) {
        return aulaConsultaPort.buscarActivas(filtro);
    }

    @Transactional(readOnly = true)
    public Aula consultarPorId(Long idAula) {
        return aulaConsultaPort.buscarActivaPorId(idAula)
                .orElseThrow(() -> new AulaNoEncontradaException(idAula));
    }
}
