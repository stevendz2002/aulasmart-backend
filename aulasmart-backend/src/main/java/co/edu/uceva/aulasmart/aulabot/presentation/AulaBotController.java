package co.edu.uceva.aulasmart.aulabot.presentation;

import co.edu.uceva.aulasmart.aulabot.application.service.AulaBotService;
import co.edu.uceva.aulasmart.aulabot.domain.model.AulaBotNoDisponibleException;
import co.edu.uceva.aulasmart.aulabot.infrastructure.CuotaAulaBot;
import co.edu.uceva.aulasmart.aulabot.presentation.dto.MensajeAulaBotRequest;
import co.edu.uceva.aulasmart.aulas.presentation.dto.AulaResponse;
import co.edu.uceva.aulasmart.aulabot.domain.model.FiltroAulaBot;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Chat informativo: respuestas por plantillas y sugerencias estructuradas. */
@RestController
@RequestMapping("/api/aulabot")
public class AulaBotController {
    private static final String COOKIE_VISITANTE = "aulasmart_chat_visitante";
    private final AulaBotService service;
    private final CuotaAulaBot quota;
    public AulaBotController(AulaBotService service,CuotaAulaBot quota) { this.service=service; this.quota=quota; }
    public record Respuesta(String texto,String estado,List<AulaResponse> aulas,Instant inicio,Instant fin,
                            Instant verificadoEn,FiltroAulaBot filtros,int total,List<String> sugerencias) {}
    @PostMapping("/mensajes")
    public ResponseEntity<Respuesta> enviar(@RequestBody MensajeAulaBotRequest request, Principal principal,
            @CookieValue(name = COOKIE_VISITANTE, required = false) String cookie, HttpServletRequest http) {
        ResponseCookie nuevaCookie = null;
        try {
            boolean interpreta = request.accion() == null;
            String identidad;
            if (principal == null) {
                String visitante = idVisitante(cookie).orElseGet(() -> UUID.randomUUID().toString());
                identidad = "visitante:" + visitante;
                quota.consumirVisitante(identidad, interpreta);
                if (!visitante.equals(cookie)) {
                    nuevaCookie = ResponseCookie.from(COOKIE_VISITANTE, visitante)
                            .httpOnly(true).secure(http.isSecure()).sameSite("Strict")
                            .path("/api/aulabot").maxAge(Duration.ofMinutes(30)).build();
                }
            } else {
                identidad = principal.getName();
                if (interpreta) quota.consumir(identidad);
            }
            var resultado = interpreta
                    ? service.enviar(identidad, request.reiniciar(), request.mensaje())
                    : service.accionar(identidad, request.accion());
            return response(resultado, nuevaCookie);
        } catch (AulaBotNoDisponibleException e) {
            return respuesta(nuevaCookie).body(new Respuesta(
                    "El intérprete no está disponible ahora o alcanzaste el límite temporal. Conservo tu búsqueda; continúa con las opciones.",
                    "ACLARACION",List.of(),null,null,null,null,0,List.of("BUSCAR_AULA","CONSULTAR_DISPONIBILIDAD","VER_OPCIONES")));
        }
    }
    private Optional<String> idVisitante(String value) {
        if (value == null || value.length() != 36) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value).toString()).filter(value::equals);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private ResponseEntity.BodyBuilder respuesta(ResponseCookie cookie) {
        var respuesta = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        return cookie == null ? respuesta : respuesta.header(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseEntity<Respuesta> response(AulaBotService.Resultado result, ResponseCookie cookie) {
        return respuesta(cookie).body(new Respuesta(result.texto(),result.estado(),
                result.aulas().stream().map(AulaResponse::fromDomain).toList(),result.inicio(),result.fin(),result.verificadoEn(),
                result.filtros(),result.total(),result.sugerencias()));
    }
}
