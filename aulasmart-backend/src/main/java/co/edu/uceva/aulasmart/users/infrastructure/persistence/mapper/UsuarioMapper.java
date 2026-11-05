package co.edu.uceva.aulasmart.users.infrastructure.persistence.mapper;

import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.RolEntity;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.UsuarioEntity;

import java.util.stream.Collectors;

/** Mapeador Entity de JPA ↔ modelo de dominio de usuarios. */
public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static PerfilUsuario toPerfil(UsuarioEntity entidad) {
        return new PerfilUsuario(
                entidad.getId(),
                entidad.getNombre(),
                entidad.getCorreo(),
                entidad.isActivo(),
                entidad.getRoles().stream()
                        .map(RolEntity::getNombre)
                        .toList(),
                entidad.getCreadoEn()
        );
    }
}
