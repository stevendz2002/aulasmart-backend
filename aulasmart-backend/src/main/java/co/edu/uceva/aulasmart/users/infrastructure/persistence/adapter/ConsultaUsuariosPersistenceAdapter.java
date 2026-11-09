package co.edu.uceva.aulasmart.users.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.users.application.port.ConsultaUsuariosPort;

import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.UsuarioEntity;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Component
public class ConsultaUsuariosPersistenceAdapter implements ConsultaUsuariosPort {

    private final UsuarioJpaRepository repository;

    public ConsultaUsuariosPersistenceAdapter(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioAutenticado> buscarPorCorreo(String correo) {
        return repository.findByCorreoIgnoreCase(correo)
                .filter(UsuarioEntity::isActivo)
                .map(usuario -> new UsuarioAutenticado(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getRol().getNombre()
                ));
    }
}
