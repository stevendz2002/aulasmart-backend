package co.edu.uceva.aulasmart.aulas.presentation;

import co.edu.uceva.aulasmart.aulas.application.service.ConsultarAulasService;
import co.edu.uceva.aulasmart.aulas.application.service.DisponibilidadAulaService;
import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaFiltro;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.aulas.presentation.dto.AulaResponse;
import co.edu.uceva.aulasmart.aulas.presentation.dto.DisponibilidadResponse;
import co.edu.uceva.aulasmart.aulas.presentation.dto.OcupacionResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {
    private final ConsultarAulasService consultarAulasService;
    private final DisponibilidadAulaService disponibilidadAulaService;
    private final HorarioAcademicoPort horarioAcademicoPort;

    public AulaController(
            ConsultarAulasService consultarAulasService,
            DisponibilidadAulaService disponibilidadAulaService,
            HorarioAcademicoPort horarioAcademicoPort
    ) {
        this.consultarAulasService = consultarAulasService;
        this.disponibilidadAulaService = disponibilidadAulaService;
        this.horarioAcademicoPort = horarioAcademicoPort;
    }

    @GetMapping
    public List<AulaResponse> consultar(
            @RequestParam(required = false) Long idEdificio,
            @RequestParam(required = false) Long idTipoAula,
            @RequestParam(required = false) Long idDependencia,
            @RequestParam(required = false) Integer capacidadMinima
    ) {
        var filtro = new AulaFiltro(idEdificio, idTipoAula, idDependencia, capacidadMinima);
        return consultarAulasService.consultar(filtro).stream()
                .map(AulaResponse::fromDomain)
                .toList();
    }

    @GetMapping("/disponibles")
    public DisponibilidadResponse consultarDisponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fin,
            @RequestParam(required = false) Long idEdificio,
            @RequestParam(required = false) Long idTipoAula,
            @RequestParam(required = false) Long idDependencia,
            @RequestParam(required = false) Integer capacidadMinima
    ) {
        var filtro = new AulaFiltro(idEdificio, idTipoAula, idDependencia, capacidadMinima);
        var intervalo = new Intervalo(inicio.toInstant(), fin.toInstant());
        return DisponibilidadResponse.fromDomain(disponibilidadAulaService.consultar(filtro, intervalo));
    }

    @GetMapping("/{idAula}/ocupacion")
    public List<OcupacionResponse> ocupacion(
            @PathVariable Long idAula,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fin
    ) {
        var intervalo = new Intervalo(inicio.toInstant(), fin.toInstant());
        return horarioAcademicoPort.buscarOcupacionDeAula(idAula, intervalo).stream()
                .map(o -> new OcupacionResponse(o.inicio(), o.fin(), o.detalles()))
                .toList();
    }
}
