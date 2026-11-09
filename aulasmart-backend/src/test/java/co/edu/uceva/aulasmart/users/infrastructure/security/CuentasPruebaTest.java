package co.edu.uceva.aulasmart.users.infrastructure.security;

import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.UsuarioEntity;
import co.edu.uceva.aulasmart.users.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CuentasPruebaTest {
    @Test
    void cuentasSinteticasRequierenHabilitacionExplicita() {
        var repository = mock(UsuarioJpaRepository.class);
        String correo = "admin@prueba.aulasmart.local";
        var usuario = UsuarioEntity.registrar("Prueba", correo, "hash", Instant.now());
        var rol = mock(co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.RolEntity.class);
        when(rol.getNombre()).thenReturn("ADMINISTRADOR");
        usuario.asignarRol(rol);
        when(repository.findByCorreoIgnoreCase(correo)).thenReturn(Optional.of(usuario));
        assertThrows(UsernameNotFoundException.class, () -> new JpaUserDetailsService(repository, false).loadUserByUsername(correo));
        assertEquals(correo, new JpaUserDetailsService(repository, true).loadUserByUsername(correo).getUsername());
    }
}
