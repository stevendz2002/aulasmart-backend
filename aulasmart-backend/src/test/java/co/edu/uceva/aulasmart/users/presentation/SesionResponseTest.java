package co.edu.uceva.aulasmart.users.presentation;

import co.edu.uceva.aulasmart.users.presentation.dto.SesionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SesionResponseTest {

    @Test
    void derivaElCorreoLosRolesYElNombreRealDeLaCuenta() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "docente@prueba.aulasmart.local",
                "n/a",
                List.of(
                        new SimpleGrantedAuthority("ROLE_DOCENTE")

                )
        );

        var sesion = SesionResponse.desde(authentication, "PRUEBA Docente");

        assertEquals("docente@prueba.aulasmart.local", sesion.correo());
        assertEquals("PRUEBA Docente", sesion.nombre());
        assertEquals("DOCENTE", sesion.rol());
    }

    @Test
    void admiteNombreNuloCuandoLaCuentaNoSeResuelveEnLaPeticion() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "docente@prueba.aulasmart.local",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"))
        );

        var sesion = SesionResponse.desde(authentication, null);

        assertEquals("docente@prueba.aulasmart.local", sesion.correo());
        assertEquals(null, sesion.nombre());
        assertEquals("DOCENTE", sesion.rol());
    }
}
