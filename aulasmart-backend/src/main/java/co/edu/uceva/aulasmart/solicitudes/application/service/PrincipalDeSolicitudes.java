package co.edu.uceva.aulasmart.solicitudes.application.service;

import co.edu.uceva.aulasmart.users.application.port.ConsultaUsuariosPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Resuelve el principal autenticado para los casos de uso de solicitudes:
 * entrega el identificador del usuario y si actúa como docente o como
 * revisor según los roles vigentes en la base.
 */
@Service
public class PrincipalDeSolicitudes {

    public record UsuarioActivo(Long id, Set<String> roles) {
        public boolean esDocente() {
            return roles.contains("DOCENTE");
        }

        public boolean esRevisor() {
            return roles.contains("ADMINISTRATIVO") || roles.contains("ADMINISTRADOR");
        }
    }

    private final ConsultaUsuariosPort consultaUsuariosPort;

    public PrincipalDeSolicitudes(ConsultaUsuariosPort consultaUsuariosPort) {
        this.consultaUsuariosPort = consultaUsuariosPort;
    }

    public UsuarioActivo resolver(Authentication authentication) {
        return consultaUsuariosPort.buscarPorCorreo(authentication.getName())
                .map(usuario -> new UsuarioActivo(usuario.id(), usuario.roles()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario autenticado no está registrado."));
    }
}
