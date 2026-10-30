package co.edu.uceva.aulasmart.integrations.siga.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.CatalogoSigaPort;
import co.edu.uceva.aulasmart.aulas.application.port.RegistroSincronizacionPort;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaExterna;
import co.edu.uceva.aulasmart.integrations.siga.infrastructure.http.SigaRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizadorDeAulasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

    @Mock
    private SigaRestClient cliente;
    @Mock
    private CatalogoSigaPort catalogoSigaPort;
    @Mock
    private RegistroSincronizacionPort registroSincronizacionPort;

    private SincronizadorDeAulasService service;

    @BeforeEach
    void configurar() {
        service = new SincronizadorDeAulasService(
                cliente,
                catalogoSigaPort,
                registroSincronizacionPort,
                Clock.fixed(AHORA, ZoneOffset.UTC)
        );
    }

    @Test
    void aplicaCatalogoYRegistraLaCorridaExitosa() {
        when(registroSincronizacionPort.registrarInicio(SincronizadorDeAulasService.RECURSO))
                .thenReturn(7L);
        when(cliente.listarAulas()).thenReturn(List.of(externa(195), externa(406)));
        when(catalogoSigaPort.aplicarAulasExternas(List.of(externa(195), externa(406)))).thenReturn(2);

        var resultado = service.sincronizar();

        assertEquals(SincronizadorDeAulasService.RECURSO, resultado.recurso());
        assertEquals("EXITOSA", resultado.estado());
        assertEquals(2, resultado.registrosProcesados());
        assertEquals(AHORA, resultado.iniciadaEn());
        assertEquals(AHORA, resultado.finalizadaEn());
        assertEquals(0, resultado.duracionMilisegundos());
        verify(registroSincronizacionPort).registrarFin(7L, "EXITOSA", 2, null);
    }

    @Test
    void registraLaCorridaComoFallidaConCodigosDuplicadosYNoAplicaNada() {
        when(registroSincronizacionPort.registrarInicio(SincronizadorDeAulasService.RECURSO))
                .thenReturn(7L);
        when(cliente.listarAulas()).thenReturn(List.of(externa(406), externa(406)));

        var resultado = service.sincronizar();

        assertEquals("FALLIDA", resultado.estado());
        assertEquals(0, resultado.registrosProcesados());
        verifyNoInteractions(catalogoSigaPort);
        verify(registroSincronizacionPort).registrarFin(eq(7L), eq("FALLIDA"), eq(0), contains("repetidos"));
    }

    @Test
    void registraLaCorridaComoFallidaSiLaConsultaFalla() {
        when(registroSincronizacionPort.registrarInicio(SincronizadorDeAulasService.RECURSO))
                .thenReturn(7L);
        when(cliente.listarAulas()).thenThrow(new IllegalStateException("conexión rechazada"));

        var resultado = service.sincronizar();

        assertEquals("FALLIDA", resultado.estado());
        verifyNoInteractions(catalogoSigaPort);
        verify(registroSincronizacionPort).registrarFin(eq(7L), eq("FALLIDA"), eq(0), contains("conexión rechazada"));
    }

    private AulaExterna externa(long codigo) {
        return new AulaExterna(
                codigo,
                "PRUEBA Aula " + codigo,
                -48L,
                "PRUEBA Edificio",
                30,
                -14L,
                "PRUEBA Dependencia",
                -1L,
                "PRUEBA Tipo"
        );
    }
}
