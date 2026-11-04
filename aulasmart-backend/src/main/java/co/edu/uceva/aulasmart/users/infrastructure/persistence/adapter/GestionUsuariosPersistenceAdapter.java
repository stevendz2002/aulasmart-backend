package co.edu.uceva.aulasmart.users.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.users.application.port.GestionUsuariosPort;
import co.edu.uceva.aulasmart.users.domain.exception.RolInvalidoException;
import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.RolEntity;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.UsuarioEntity;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.mapper.UsuarioMapper;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.repository.RolJpaRepository;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación persistente del directorio de usuarios sobre JPA.
 * Transaccional: la creación guarda la cuenta y todos sus roles atómicamente.
 */
@Component
public class GestionUsuariosPersistenceAdapter implements GestionUsuariosPort {

    private final UsuarioJpaRepository usuarios;
    private final RolJpaRepository roles;

    public GestionUsuariosPersistenceAdapter(
            UsuarioJpaRepository usuarios,
            RolJpaRepository roles
    ) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerfilUsuario> listar() {
        return usuarios.findAllByOrderByCreadoEnDesc().stream()
                .map(UsuarioMapper::toPerfil)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCorreo(String correo) {
        return usuarios.existsByCorreoIgnoreCase(correo);
    }

    @Override
    @Transactional
    public PerfilUsuario crear(String nombre, String correo, String claveCodificada, List<String> rolesSolicitados) {
        var rolesAsignados = new LinkedHashSet<RolEntity>();
        for (String rol : rolesSolicitados) {
            var entidad = roles.findByNombreIgnoreCase(rol)
                    .orElseThrow(() -> new RolInvalidoException(rol));
            rolesAsignados.add(entidad);
        }

        var cuenta = UsuarioEntity.registrar(nombre, correo, claveCodificada, Instant.now());
        cuenta.asignarRoles(rolesAsignados);
        var guardada = usuarios.save(cuenta);

        // Rehidrata: LA relación LAZY no se resolvió en la entidad guardada.
        return UsuarioMapper.toPerfil(usuarios.findByCorreoIgnoreCase(correo).orElse(guardada));
    }
}
