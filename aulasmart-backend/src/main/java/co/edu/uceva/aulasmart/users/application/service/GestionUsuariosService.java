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
 * Casos de uso del directorio de usuarios. Reglas de negocio: nombre y correo
 * obligatorios con formato válido, clave de al menos 8 caracteres, roles del
 * catálogo real y correo sin cuenta previa. En la edición, un campo ausente
 * (null) no se modifica y el administrador nunca desactiva su propia cuenta.
 * El acceso (quién puede invocar estas operaciones) lo decide la
 * configuración de seguridad: exclusivamente el rol ADMINISTRADOR.
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
        var nombreLimpio = textoONulo(nombre);
        var correoLimpio = textoONulo(correo);

        if (nombreLimpio == null || nombreLimpio.length() > 120) {
            throw new RegistroInvalidoException("El nombre es obligatorio (1 a 120 caracteres).");
        }
        if (!correoValido(correoLimpio)) {
            throw new RegistroInvalidoException("El correo es obligatorio y debe tener formato institucional.");
        }
        if (clave == null || clave.length() < 8) {
            throw new RegistroInvalidoException("La clave de la cuenta debe tener al menos 8 caracteres.");
        }
        var rolesLimpios = normalizarRoles(roles);
        if (rolesLimpios.isEmpty()) {
            throw new RegistroInvalidoException("La cuenta necesita al menos un rol del catálogo.");
        }

        if (gestionUsuariosPort.existeCorreo(correoLimpio)) {
            throw new CorreoYaRegistradoException(correoLimpio);
        }

        var claveCodificada = codificadorClavesPort.codificar(clave);
        return gestionUsuariosPort.crear(nombreLimpio, correoLimpio, claveCodificada, List.copyOf(rolesLimpios));
    }

    /**
     * Edita una cuenta existente: cada campo en null se conserva tal cual.
     * Regla de protección operativa: el administrador no puede desactivar su
     * propia cuenta (evita quedar sin acceso al directorio).
     */
    public PerfilUsuario editar(
            Long idUsuario,
            String nombre,
            List<String> roles,
            Boolean activo,
            Long idSolicitante
    ) {
        if (idUsuario == null) {
            throw new RegistroInvalidoException("El identificador de la cuenta es obligatorio.");
        }
        String nombreLimpio = null;
        if (nombre != null) {
            nombreLimpio = nombre.trim();
            if (nombreLimpio.isEmpty() || nombreLimpio.length() > 120) {
                throw new RegistroInvalidoException("El nombre debe tener entre 1 y 120 caracteres.");
            }
        }
        var rolesLimpios = roles == null ? null : normalizarRoles(roles);
        if (rolesLimpios != null && rolesLimpios.isEmpty()) {
            throw new RegistroInvalidoException("La cuenta necesita al menos un rol del catálogo.");
        }
        if (Boolean.FALSE.equals(activo) && idUsuario.equals(idSolicitante)) {
            throw new RegistroInvalidoException("El administrador no puede desactivar su propia cuenta.");
        }

        return gestionUsuariosPort.editar(
                idUsuario,
                nombreLimpio,
                rolesLimpios == null ? null : List.copyOf(rolesLimpios),
                activo
        );
    }

    /** Normaliza, deduplica y valida los roles contra el catálogo real. */
    private Set<String> normalizarRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
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
        return rolesLimpios;
    }

    private String textoONulo(String texto) {
        var limpio = texto == null ? "" : texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private boolean correoValido(String correo) {
        return correo != null && correo.length() <= 254 && correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
