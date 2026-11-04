package co.edu.uceva.aulasmart.users.presentation;

import co.edu.uceva.aulasmart.users.application.port.ConsultaUsuariosPort;
import co.edu.uceva.aulasmart.users.application.service.GestionUsuariosService;
import co.edu.uceva.aulasmart.users.presentation.dto.RegistroUsuarioRequest;
import co.edu.uceva.aulasmart.users.presentation.dto.SesionResponse;
import co.edu.uceva.aulasmart.users.presentation.dto.UsuarioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Directorio de usuarios. `GET /api/usuarios` y `POST /api/usuarios` son
 * exclusivas del rol ADMINISTRADOR (configuración de seguridad); el perfil
 * `whoami` sigue abierto a cualquier sesión autenticada.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final GestionUsuariosService gestionUsuariosService;
    private final ConsultaUsuariosPort consultaUsuariosPort;

    public UsuarioController(
            GestionUsuariosService gestionUsuariosService,
            ConsultaUsuariosPort consultaUsuariosPort
    ) {
        this.gestionUsuariosService = gestionUsuariosService;
        this.consultaUsuariosPort = consultaUsuariosPort;
    }

    /** Directorio completo del sistema (solo ADMINISTRADOR). */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return gestionUsuariosService.listar().stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    /** Crear una cuenta (solo ADMINISTRADOR): las personas no se autorregistro. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@RequestBody RegistroUsuarioRequest request) {
        var perfil = gestionUsuariosService.crear(
                request.nombre(),
                request.correo(),
                request.clave(),
                request.roles()
        );
        return UsuarioResponse.desde(perfil);
    }

    @GetMapping("/whoami")
    public SesionResponse whoami(Authentication authentication) {
        var nombre = consultaUsuariosPort.buscarPorCorreo(authentication.getName())
                .map(ConsultaUsuariosPort.UsuarioAutenticado::nombre)
                .orElse(null);
        return SesionResponse.desde(authentication, nombre);
    }
}
