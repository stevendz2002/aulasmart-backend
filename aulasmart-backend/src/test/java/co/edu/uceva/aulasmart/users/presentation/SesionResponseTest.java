package co.edu.uceva.aulasmart.users.presentation;

import co.edu.uceva.aulasmart.users.presentation.dto.SesionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SesionResponseTest {

    @Test
    void derivaElCorreoYLosRolesDeLasAutoridadesDeLaPeticion() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "docente@prueba.aulasmart.local",
                "n/a",
                List.of(
                        new SimpleGrantedAuthority("ROLE_DOCENTE"),
                        new SimpleGrantedAuthority("ROLE_ESTUDIANTE")
                )
        );

        var sesion = SesionResponse.desde(authentication);

        assertEquals("docente@prueba.aulasmart.local", sesion.correo());
        assertEquals(List.of("DOCENTE", "ESTUDIANTE"), sesion.roles());
    }
}
