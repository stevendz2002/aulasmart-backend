package co.edu.uceva.aulasmart.solicitudes.application.service;

import co.edu.uceva.aulasmart.users.application.port.ConsultaUsuariosPort;

import org.springframework.stereotype.Service;

/**
 * Resuelve el principal autenticado para los casos de uso de solicitudes:
 * entrega el identificador del usuario y si actúa como docente o como
 * revisor según los roles vigentes en la base.
 */
@Service
public class PrincipalDeSolicitudes {

    public record UsuarioActivo(Long id, String rol) {
        public boolean esDocente() {
            return "DOCENTE".equals(rol);
        }

        public boolean esRevisor() {
            return "ADMINISTRATIVO".equals(rol) || "ADMINISTRADOR".equals(rol);
        }
    }

    private final ConsultaUsuariosPort consultaUsuariosPort;

    public PrincipalDeSolicitudes(ConsultaUsuariosPort consultaUsuariosPort) {
        this.consultaUsuariosPort = consultaUsuariosPort;
    }

    public UsuarioActivo resolver(String correo) {
        return consultaUsuariosPort.buscarPorCorreo(correo)
                .map(usuario -> new UsuarioActivo(usuario.id(), usuario.rol()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario autenticado no está registrado."));
    }
}
