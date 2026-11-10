package co.edu.uceva.aulasmart.aulabot.presentation;

import co.edu.uceva.aulasmart.aulabot.application.service.AulaBotService;
import co.edu.uceva.aulasmart.aulabot.domain.model.AulaBotNoDisponibleException;
import co.edu.uceva.aulasmart.aulabot.infrastructure.CuotaAulaBot;
import co.edu.uceva.aulasmart.aulabot.presentation.dto.MensajeAulaBotRequest;
import co.edu.uceva.aulasmart.aulas.presentation.dto.AulaResponse;
import co.edu.uceva.aulasmart.aulabot.domain.model.FiltroAulaBot;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Chat informativo: respuestas por plantillas y sugerencias estructuradas. */
@RestController
@RequestMapping("/api/aulabot")
public class AulaBotController {
    private final AulaBotService service;
    private final CuotaAulaBot quota;
    public AulaBotController(AulaBotService service,CuotaAulaBot quota) { this.service=service; this.quota=quota; }
    public record Respuesta(String texto,String estado,List<AulaResponse> aulas,Instant inicio,Instant fin,
                            Instant verificadoEn,FiltroAulaBot filtros,int total,List<String> sugerencias) {}
    @PostMapping("/mensajes")
    public ResponseEntity<Respuesta> enviar(@RequestBody MensajeAulaBotRequest request,Authentication auth) {
        try {
            if (request.accion()!=null) {
                return response(service.accionar(auth.getName(),request.accion()));
            }
            quota.consumir(auth.getName());
            return response(service.enviar(auth.getName(),request.reiniciar(),request.mensaje()));
        } catch (AulaBotNoDisponibleException e) {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Respuesta(
                    "El intérprete no está disponible ahora o alcanzaste el límite temporal. Conservo tu búsqueda; continúa con las opciones.",
                    "ACLARACION",List.of(),null,null,null,null,0,List.of("BUSCAR_AULA","CONSULTAR_DISPONIBILIDAD","VER_OPCIONES")));
        }
    }
    private ResponseEntity<Respuesta> response(AulaBotService.Resultado result) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Respuesta(result.texto(),result.estado(),
                result.aulas().stream().map(AulaResponse::fromDomain).toList(),result.inicio(),result.fin(),result.verificadoEn(),
                result.filtros(),result.total(),result.sugerencias()));
    }
}
