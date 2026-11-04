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
        when(gestionUsuariosPort.crear("Laura Torres", "nueva.docente@uceva.edu.co", "BCRYPT", List.of("DOCENTE")))
                .thenReturn(new PerfilUsuario(9L, "Laura Torres", "nueva.docente@uceva.edu.co", true,
                        List.of("DOCENTE"), AHORA));

        var perfil = service.crear("Laura Torres", "nueva.docente@uceva.edu.co", "claveSegura1", List.of("docente"));

        assertEquals(9L, perfil.id());
        assertEquals(List.of("DOCENTE"), perfil.roles());
        verify(gestionUsuariosPort).crear("Laura Torres", "nueva.docente@uceva.edu.co", "BCRYPT", List.of("DOCENTE"));
    }

    @Test
    void rechazaCorreosConFormatoInvalido() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.crear("Nombre", "sin-arroba", "claveSegura1", List.of("DOCENTE")));
        verify(gestionUsuariosPort, never()).crear(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void rechazaClavesMenoresAOchoCaracteres() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.crear("Nombre", "nueva@uceva.edu.co", "123", List.of("DOCENTE")));
        verify(codificadorClavesPort, never()).codificar(anyString());
    }

    @Test
    void rechazaRolesFueraDelCatalogo() {
        assertThrows(RolInvalidoException.class,
                () -> service.crear("Nombre", "nueva@uceva.edu.co", "claveSegura1", List.of("DECANO")));
        verify(gestionUsuariosPort, never()).crear(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void rechazaCuentasParaCorreosYaRegistrados() {
        when(gestionUsuariosPort.existeCorreo("revisor@prueba.aulasmart.local")).thenReturn(true);

        assertThrows(CorreoYaRegistradoException.class,
                () -> service.crear("Revisor Segundo", "revisor@prueba.aulasmart.local", "claveSegura1", List.of("ADMINISTRATIVO")));

        verify(gestionUsuariosPort, never()).crear(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void listaElDirectorioCompleto() {
        when(gestionUsuariosPort.listar()).thenReturn(List.of(
                new PerfilUsuario(9003L, "PRUEBA Administrador", "admin@prueba.aulasmart.local", true,
                        List.of("ADMINISTRADOR"), AHORA)
        ));

        var resultado = service.listar();

        assertEquals(1, resultado.size());
        assertEquals(9003L, resultado.get(0).id());
        assertEquals("ADMINISTRADOR", resultado.get(0).roles().get(0));
        verify(gestionUsuariosPort).listar();
    }

    @Test
    void editaElNombreYLosRolesDeUnaCuenta() {
        when(gestionUsuariosPort.editar(7L, "Laura Torres Gómez", List.of("DOCENTE", "ESTUDIANTE"), null))
                .thenReturn(new PerfilUsuario(7L, "Laura Torres Gómez", "laura@uceva.edu.co", true,
                        List.of("DOCENTE", "ESTUDIANTE"), AHORA));

        var perfil = service.editar(7L, " Laura Torres Gómez ", List.of("docente", "ESTUDIANTE"), null, 9003L);

        assertEquals("Laura Torres Gómez", perfil.nombre());
        verify(gestionUsuariosPort).editar(7L, "Laura Torres Gómez", List.of("DOCENTE", "ESTUDIANTE"), null);
    }

    @Test
    void desactivaUnaCuentaQueNoEsLaDelPropioAdministrador() {
        when(gestionUsuariosPort.editar(7L, null, null, false)).thenReturn(new PerfilUsuario(
                7L, "Cuenta", "cuenta@uceva.edu.co", false, List.of("DOCENTE"), AHORA));

        var perfil = service.editar(7L, null, null, false, 9003L);

        assertEquals(false, perfil.activo());
        verify(gestionUsuariosPort).editar(7L, null, null, false);
    }

    @Test
    void rechazaQueElAdministradorDesactiveSuPropiaCuenta() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.editar(9003L, null, null, false, 9003L));

        verify(gestionUsuariosPort, never()).editar(anyLong(), anyString(), anyList(), any());
    }

    @Test
    void rechazaEdicionesConRolesFueraDelCatalogo() {
        assertThrows(RolInvalidoException.class,
                () -> service.editar(7L, null, List.of("DECANO"), null, 9003L));

        verify(gestionUsuariosPort, never()).editar(anyLong(), anyString(), anyList(), any());
    }

    @Test
    void rechazaEdicionesConNombreVacioOExcesivo() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.editar(7L, "   ", null, null, 9003L));
        assertThrows(RegistroInvalidoException.class,
                () -> service.editar(7L, "x".repeat(121), null, null, 9003L));

        verify(gestionUsuariosPort, never()).editar(anyLong(), anyString(), anyList(), any());
    }

    @Test
    void rechazaEdicionesSinIdentificadorDeCuenta() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.editar(null, "Nombre", null, null, 9003L));
    }
}
