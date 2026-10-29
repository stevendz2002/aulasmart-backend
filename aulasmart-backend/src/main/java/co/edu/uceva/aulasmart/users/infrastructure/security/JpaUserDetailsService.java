package co.edu.uceva.aulasmart.users.infrastructure.security;

import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.RolEntity;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaUserDetailsService implements UserDetailsService {
    private final UsuarioJpaRepository repository;

    public JpaUserDetailsService(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var usuario = repository.findByCorreoIgnoreCase(username)
                .filter(entity -> entity.isActivo())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inactivo."));
        String[] roles = usuario.getRoles().stream()
                .map(RolEntity::getNombre)
                .toArray(String[]::new);
        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .roles(roles)
                .build();
    }
}
