package co.edu.uceva.aulasmart.users.infrastructure.security;


import co.edu.uceva.aulasmart.users.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaUserDetailsService implements UserDetailsService {
    private final UsuarioJpaRepository repository;
    private final boolean permitirCuentasPrueba;

    public JpaUserDetailsService(UsuarioJpaRepository repository,
            @Value("${aulasmart.security.allow-test-accounts:false}") boolean permitirCuentasPrueba) {
        this.repository = repository;
        this.permitirCuentasPrueba = permitirCuentasPrueba;
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var usuario = repository.findByCorreoIgnoreCase(username)
                .filter(entity -> entity.isActivo())
                .filter(entity -> permitirCuentasPrueba || !entity.getCorreo().toLowerCase(java.util.Locale.ROOT)
                        .endsWith("@prueba.aulasmart.local"))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inactivo."));
        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRol().getNombre())
                .build();
    }
}
