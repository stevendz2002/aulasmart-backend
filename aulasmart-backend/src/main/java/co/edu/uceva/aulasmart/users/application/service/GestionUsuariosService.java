package co.edu.uceva.aulasmart.users.application.service;

import co.edu.uceva.aulasmart.users.application.port.CodificadorClavesPort;
import co.edu.uceva.aulasmart.users.application.port.GestionUsuariosPort;
import co.edu.uceva.aulasmart.users.domain.exception.CorreoYaRegistradoException;
import co.edu.uceva.aulasmart.users.domain.exception.RegistroInvalidoException;
import co.edu.uceva.aulasmart.users.domain.exception.RolInvalidoException;
import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Caso de uso del directorio de usuarios. Reglas de negocio:
 * nombre y correo obligatorios con formato válido, clave de al menos 8
 * caracteres, roles del catálogo real y correo sin cuenta previa.
 * El acceso (quién puede invocar la creacion) lo decide la configuración
 * de seguridad: exclusivamente el rol ADMINISTRADOR.
 */
@Service
public class GestionUsuariosService {

    /** Catálogo de roles vigentes en la base (tabla rol del esquema V1). */
    static final Set<String> ROLES_VALIDOS = Set.of(
            "ADMINISTRADOR", "ADMINISTRATIVO", "DOCENTE", "ESTUDIANTE"
    );

    private final GestionUsuariosPort gestionUsuariosPort;
    private final CodificadorClavesPort codificadorClavesPort;

    public GestionUsuariosService(
            GestionUsuariosPort gestionUsuariosPort,
            CodificadorClavesPort codificadorClavesPort
    ) {
        this.gestionUsuariosPort = gestionUsuariosPort;
        this.codificadorClavesPort = codificadorClavesPort;
    }

    public List<PerfilUsuario> listar() {
        return gestionUsuariosPort.listar();
    }

    public PerfilUsuario crear(String nombre, String correo, String clave, List<String> roles) {
        var nombreLimpio = nombre == null ? "" : nombre.trim();
        var correoLimpio = correo == null ? "" : correo.trim();

        if (nombreLimpio.isEmpty() || nombreLimpio.length() > 120) {
            throw new RegistroInvalidoException("El nombre es obligatorio (1 a 120 caracteres).");
        }
        if (!correoValido(correoLimpio)) {
            throw new RegistroInvalidoException("El correo es obligatorio y debe tener formato institucional.");
        }
        if (clave == null || clave.length() < 8) {
            throw new RegistroInvalidoException("La clave de la cuenta debe tener al menos 8 caracteres.");
        }
        if (roles == null || roles.isEmpty()) {
            throw new RegistroInvalidoException("La cuenta necesita al menos un rol.");
        }

        var rolesLimpios = new LinkedHashSet<String>();
        for (String rol : roles) {
            var rolLimpio = rol == null ? "" : rol.trim().toUpperCase(Locale.ROOT);
            if (rolLimpio.isEmpty()) {
                continue;
            }
            if (!ROLES_VALIDOS.contains(rolLimpio)) {
                throw new RolInvalidoException(rolLimpio);
            }
            rolesLimpios.add(rolLimpio);
        }
        if (rolesLimpios.isEmpty()) {
            throw new RegistroInvalidoException("La cuenta necesita al menos un rol del catálogo.");
        }

        if (gestionUsuariosPort.existeCorreo(correoLimpio)) {
            throw new CorreoYaRegistradoException(correoLimpio);
        }

        var claveCodificada = codificadorClavesPort.codificar(clave);
        return gestionUsuariosPort.crear(nombreLimpio, correoLimpio, claveCodificada, List.copyOf(rolesLimpios));
    }

    private boolean correoValido(String correo) {
        return correo.length() <= 254 && correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
