package co.edu.uceva.aulasmart.users.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.users.application.port.GestionUsuariosPort;
import co.edu.uceva.aulasmart.users.domain.exception.RolInvalidoException;
import co.edu.uceva.aulasmart.users.domain.exception.UsuarioNoEncontradoException;
import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.RolEntity;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.UsuarioEntity;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.mapper.UsuarioMapper;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.repository.RolJpaRepository;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.List;


/**
 * Implementación persistente del directorio de usuarios sobre JPA.
 * Transaccional: la creación guarda la cuenta y su rol atómicamente.
 */
@Component
public class GestionUsuariosPersistenceAdapter implements GestionUsuariosPort {

    private final UsuarioJpaRepository usuarios;
    private final RolJpaRepository roles;
    private final boolean permitirPruebas;

    public GestionUsuariosPersistenceAdapter(
            UsuarioJpaRepository usuarios,
            RolJpaRepository roles,
            @org.springframework.beans.factory.annotation.Value("${aulasmart.security.allow-test-accounts:false}") boolean permitirPruebas
    ) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.permitirPruebas = permitirPruebas;
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
    public PerfilUsuario crear(String nombre, String correo, String claveCodificada, String rolSolicitado) {
        var cuenta = UsuarioEntity.registrar(nombre, correo, claveCodificada, Instant.now());
        cuenta.asignarRol(resolverRol(rolSolicitado));
        var guardada = usuarios.save(cuenta);

        // Rehidrata: LA relación LAZY no se resolvió en la entidad guardada.
        return UsuarioMapper.toPerfil(usuarios.findByCorreoIgnoreCase(correo).orElse(guardada));
    }

    @Override
    @Transactional
    public PerfilUsuario editar(Long idUsuario, String nombre, String rol, Boolean activo) {
        // Serializa cambios administrativos para que dos solicitudes no eliminen al último administrador.
        roles.bloquearAdministracion().orElseThrow(() -> new RolInvalidoException("ADMINISTRADOR"));
        var cuenta = usuarios.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(idUsuario));
        boolean pierdeAdministracion = cuenta.isActivo() && "ADMINISTRADOR".equals(cuenta.getRol().getNombre())
                && (permitirPruebas || !cuenta.getCorreo().toLowerCase(java.util.Locale.ROOT).endsWith("@prueba.aulasmart.local"))
                && (Boolean.FALSE.equals(activo) || (rol != null && !"ADMINISTRADOR".equals(rol)));
        if (pierdeAdministracion && usuarios.contarAdministradoresHabilitados(permitirPruebas) <= 1) {
            throw new co.edu.uceva.aulasmart.users.domain.exception.RegistroInvalidoException(
                    "Debe permanecer al menos un administrador activo.");
        }
        if (nombre != null) {
            cuenta.actualizarNombre(nombre);
        }
        if (rol != null) {
            cuenta.asignarRol(resolverRol(rol));
        }
        if (activo != null) {
            cuenta.actualizarActivo(activo);
        }
        usuarios.save(cuenta);
        return UsuarioMapper.toPerfil(cuenta);
    }

    private RolEntity resolverRol(String rolSolicitado) {
        return roles.findByNombreIgnoreCase(rolSolicitado)
                .orElseThrow(() -> new RolInvalidoException(rolSolicitado));
    }
}
