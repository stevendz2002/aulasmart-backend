package co.edu.uceva.aulasmart.users.application.service;

import co.edu.uceva.aulasmart.users.application.port.CodificadorClavesPort;
import co.edu.uceva.aulasmart.users.application.port.GestionUsuariosPort;
import co.edu.uceva.aulasmart.users.domain.exception.CorreoYaRegistradoException;
import co.edu.uceva.aulasmart.users.domain.exception.RegistroInvalidoException;
import co.edu.uceva.aulasmart.users.domain.exception.RolInvalidoException;
import co.edu.uceva.aulasmart.users.domain.model.PerfilUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionUsuariosServiceTest {
    @Test
    void rechazaRolAusenteVacioOCombinado() {
        for (String rol : new String[]{null, "", "  ", "DOCENTE,ESTUDIANTE"}) {
            assertThrows(RuntimeException.class, () -> service.crear("Cuenta", "cuenta@uceva.edu.co", "claveSegura1", rol));
        }
    }

    private static final Instant AHORA = Instant.parse("2026-11-03T13:00:00Z");

    @Mock
    private GestionUsuariosPort gestionUsuariosPort;
    @Mock
    private CodificadorClavesPort codificadorClavesPort;

    private GestionUsuariosService service;

    @BeforeEach
    void configurar() {
        service = new GestionUsuariosService(gestionUsuariosPort, codificadorClavesPort);
    }

    @Test
    void creaLaCuentaConClaveCodificadaYRolesDelCatalogo() {
        when(gestionUsuariosPort.existeCorreo("nueva.docente@uceva.edu.co")).thenReturn(false);
        when(codificadorClavesPort.codificar("claveSegura1")).thenReturn("BCRYPT");
        when(gestionUsuariosPort.crear("Laura Torres", "nueva.docente@uceva.edu.co", "BCRYPT", "DOCENTE"))
                .thenReturn(new PerfilUsuario(9L, "Laura Torres", "nueva.docente@uceva.edu.co", true,
                        "DOCENTE", AHORA));

        var perfil = service.crear("Laura Torres", "nueva.docente@uceva.edu.co", "claveSegura1", "docente");

        assertEquals(9L, perfil.id());
        assertEquals("DOCENTE", perfil.rol());
        verify(gestionUsuariosPort).crear("Laura Torres", "nueva.docente@uceva.edu.co", "BCRYPT", "DOCENTE");
    }

    @Test
    void rechazaCorreosConFormatoInvalido() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.crear("Nombre", "sin-arroba", "claveSegura1", "DOCENTE"));
        verify(gestionUsuariosPort, never()).crear(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rechazaClavesMenoresAOchoCaracteres() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.crear("Nombre", "nueva@uceva.edu.co", "123", "DOCENTE"));
        verify(codificadorClavesPort, never()).codificar(anyString());
    }

    @Test
    void rechazaRolesFueraDelCatalogo() {
        assertThrows(RolInvalidoException.class,
                () -> service.crear("Nombre", "nueva@uceva.edu.co", "claveSegura1", "DECANO"));
        verify(gestionUsuariosPort, never()).crear(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rechazaCuentasParaCorreosYaRegistrados() {
        when(gestionUsuariosPort.existeCorreo("revisor@prueba.aulasmart.local")).thenReturn(true);

        assertThrows(CorreoYaRegistradoException.class,
                () -> service.crear("Revisor Segundo", "revisor@prueba.aulasmart.local", "claveSegura1", "ADMINISTRATIVO"));

        verify(gestionUsuariosPort, never()).crear(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void listaElDirectorioCompleto() {
        when(gestionUsuariosPort.listar()).thenReturn(List.of(
                new PerfilUsuario(9003L, "PRUEBA Administrador", "admin@prueba.aulasmart.local", true,
                        "ADMINISTRADOR", AHORA)
        ));

        var resultado = service.listar();

        assertEquals(1, resultado.size());
        assertEquals(9003L, resultado.get(0).id());
        assertEquals("ADMINISTRADOR", resultado.get(0).rol());
        verify(gestionUsuariosPort).listar();
    }

    @Test
    void editaElNombreYLosRolesDeUnaCuenta() {
        when(gestionUsuariosPort.editar(7L, "Laura Torres Gómez", "DOCENTE", null))
                .thenReturn(new PerfilUsuario(7L, "Laura Torres Gómez", "laura@uceva.edu.co", true,
                        "DOCENTE", AHORA));

        var perfil = service.editar(7L, " Laura Torres Gómez ", "docente", null, 9003L);

        assertEquals("Laura Torres Gómez", perfil.nombre());
        verify(gestionUsuariosPort).editar(7L, "Laura Torres Gómez", "DOCENTE", null);
    }

    @Test
    void desactivaUnaCuentaQueNoEsLaDelPropioAdministrador() {
        when(gestionUsuariosPort.editar(7L, null, null, false)).thenReturn(new PerfilUsuario(
                7L, "Cuenta", "cuenta@uceva.edu.co", false, "DOCENTE", AHORA));

        var perfil = service.editar(7L, null, null, false, 9003L);

        assertEquals(false, perfil.activo());
        verify(gestionUsuariosPort).editar(7L, null, null, false);
    }

    @Test
    void rechazaQueElAdministradorDesactiveSuPropiaCuenta() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.editar(9003L, null, null, false, 9003L));

        verify(gestionUsuariosPort, never()).editar(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void rechazaEdicionesConRolesFueraDelCatalogo() {
        assertThrows(RolInvalidoException.class,
                () -> service.editar(7L, null, "DECANO", null, 9003L));

        verify(gestionUsuariosPort, never()).editar(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void rechazaEdicionesConNombreVacioOExcesivo() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.editar(7L, "   ", null, null, 9003L));
        assertThrows(RegistroInvalidoException.class,
                () -> service.editar(7L, "x".repeat(121), null, null, 9003L));

        verify(gestionUsuariosPort, never()).editar(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void rechazaEdicionesSinIdentificadorDeCuenta() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.editar(null, "Nombre", null, null, 9003L));
    }
}
