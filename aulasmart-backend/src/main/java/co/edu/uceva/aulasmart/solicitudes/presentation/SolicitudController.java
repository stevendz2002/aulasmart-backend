package co.edu.uceva.aulasmart.solicitudes.presentation;

import co.edu.uceva.aulasmart.solicitudes.application.service.ConsultarSolicitudesService;
import co.edu.uceva.aulasmart.solicitudes.application.service.PrincipalDeSolicitudes;
import co.edu.uceva.aulasmart.solicitudes.application.service.RevisarSolicitudService;
import co.edu.uceva.aulasmart.solicitudes.application.service.RegistrarSolicitudService;
import co.edu.uceva.aulasmart.solicitudes.presentation.dto.RevisionRequest;
import co.edu.uceva.aulasmart.solicitudes.presentation.dto.SolicitudRequest;
import co.edu.uceva.aulasmart.solicitudes.presentation.dto.SolicitudResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Solicitudes de reserva de aulas. Solo un DOCENTE registra solicitudes y
 * solo un revisor (ADMINISTRATIVO o ADMINISTRADOR) las decide; la consulta
 * muestra las solicitudes propias del docente o todas según el rol.
 */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final RegistrarSolicitudService registrarSolicitudService;
    private final ConsultarSolicitudesService consultarSolicitudesService;
    private final RevisarSolicitudService revisarSolicitudService;
    private final PrincipalDeSolicitudes principalDeSolicitudes;

    public SolicitudController(
            RegistrarSolicitudService registrarSolicitudService,
            ConsultarSolicitudesService consultarSolicitudesService,
            RevisarSolicitudService revisarSolicitudService,
            PrincipalDeSolicitudes principalDeSolicitudes
    ) {
        this.registrarSolicitudService = registrarSolicitudService;
        this.consultarSolicitudesService = consultarSolicitudesService;
        this.revisarSolicitudService = revisarSolicitudService;
        this.principalDeSolicitudes = principalDeSolicitudes;
    }

    @PostMapping
    public ResponseEntity<SolicitudResponse> registrar(
            @RequestBody SolicitudRequest request,
            Authentication authentication
    ) {
        var usuario = requireCreador(principalDeSolicitudes.resolver(authentication));
        var solicitud = registrarSolicitudService.registrar(
                usuario.id(),
                request.aulaId(),
                request.inicio(),
                request.fin(),
                request.motivo()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SolicitudResponse.desde(solicitud));
    }

    @GetMapping
    public List<SolicitudResponse> consultar(
            @RequestParam(required = false) String estado,
            Authentication authentication
    ) {
        var usuario = requireRevisorODocente(principalDeSolicitudes.resolver(authentication));
        return consultarSolicitudesService.consultar(usuario, estado).stream()
                .map(SolicitudResponse::desde)
                .toList();
    }

    @PostMapping("/{idSolicitud}/revision")
    public SolicitudResponse revisar(
            @PathVariable Long idSolicitud,
            @RequestBody RevisionRequest request,
            Authentication authentication
    ) {
        var usuario = requireRevisor(principalDeSolicitudes.resolver(authentication));
        var solicitud = revisarSolicitudService.revisar(idSolicitud, usuario.id(), request.decision());
        return SolicitudResponse.desde(solicitud);
    }

    /** Salvavidas del contrato de roles, además de la configuración de seguridad. */
    private PrincipalDeSolicitudes.UsuarioActivo requireCreador(PrincipalDeSolicitudes.UsuarioActivo usuario) {
        if (!usuario.esDocente() && !usuario.esRevisor()) {
            throw new AccessDeniedException("Solo un docente o un revisor registra solicitudes de reserva.");
        }
        return usuario;
    }

    private PrincipalDeSolicitudes.UsuarioActivo requireRevisor(PrincipalDeSolicitudes.UsuarioActivo usuario) {
        if (!usuario.esRevisor()) {
            throw new AccessDeniedException("Solo un revisor decide solicitudes de reserva.");
        }
        return usuario;
    }

    private PrincipalDeSolicitudes.UsuarioActivo requireRevisorODocente(PrincipalDeSolicitudes.UsuarioActivo usuario) {
        if (!usuario.esRevisor() && !usuario.esDocente()) {
            throw new AccessDeniedException(
                    "Solo un docente consulta sus solicitudes o un revisor consulta todas.");
        }
        return usuario;
    }
}
