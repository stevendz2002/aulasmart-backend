package co.edu.uceva.aulasmart.users.presentation;

import co.edu.uceva.aulasmart.users.presentation.dto.SesionResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sesion del usuario autenticado para la SPA: el correo con el que abre
 * sesion y sus roles vigentes en cada peticion (Basic no mantiene sesion).
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @GetMapping("/whoami")
    public SesionResponse whoami(Authentication authentication) {
        return SesionResponse.desde(authentication);
    }
}
