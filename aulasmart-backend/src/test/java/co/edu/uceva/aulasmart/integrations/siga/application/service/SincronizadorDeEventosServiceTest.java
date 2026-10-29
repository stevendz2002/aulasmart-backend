package co.edu.uceva.aulasmart.integrations.siga.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.RegistroSincronizacionPort;
import co.edu.uceva.aulasmart.integrations.siga.SigaPropiedades;
import co.edu.uceva.aulasmart.integrations.siga.infrastructure.http.SigaEventosRespuesta;
import co.edu.uceva.aulasmart.integrations.siga.infrastructure.http.SigaRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SincronizadorDeEventosServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");
    // Fecha institucional (America/Bogota) correspondiente a AHORA.
    private static final LocalDate HOY = LocalDate.parse("2026-10-29");
    private static final LocalDate VENTANA_INICIO = HOY.minusDays(7);
    private static final LocalDate VENTANA_FIN = HOY.plusDays(180);

    @Mock
    private SigaRestClient cliente;
    @Mock
    private RegistroSincronizacionPort registroSincronizacionPort;

    private SincronizadorDeEventosService service;

    @BeforeEach
    void configurar() {
        var propiedades = new SigaPropiedades(
                "https://prueba.aulasmart.local",
                Duration.ofSeconds(15),
                new SigaPropiedades.Eventos(7, 180),
                new SigaPropiedades.Programado("0 0 4 * * *", "America/Bogota")
        );
        service = new SincronizadorDeEventosService(
                cliente,
                registroSincronizacionPort,
                propiedades,
                Clock.fixed(AHORA, ZoneOffset.UTC)
        );
    }

    @Test
    void registraExitosaConVentanaCubiertaCuandoNoHayEventos() {
        when(registroSincronizacionPort.registrarInicio(SincronizadorDeEventosService.RECURSO,
                VENTANA_INICIO, VENTANA_FIN)).thenReturn(9L);
        when(cliente.listarEventos(VENTANA_INICIO, VENTANA_FIN))
                .thenReturn(new SigaEventosRespuesta(0, 0, ""));

        var resultado = service.sincronizar();

        assertEquals(SincronizadorDeEventosService.RECURSO, resultado.recurso());
        assertEquals("EXITOSA", resultado.estado());
        assertEquals(0, resultado.registrosProcesados());
        // La ventana declarada es la afirmación que mantiene la puerta de disponibilidad.
        verify(registroSincronizacionPort).registrarInicio(SincronizadorDeEventosService.RECURSO,
                VENTANA_INICIO, VENTANA_FIN);
        verify(registroSincronizacionPort).registrarFin(9L, "EXITOSA", 0, null);
    }

    @Test
    void registraLaCorridaComoFallidaSiLaListaDeEventosNoEstaVacia() {
        when(registroSincronizacionPort.registrarInicio(eq(SincronizadorDeEventosService.RECURSO),
                eq(VENTANA_INICIO), eq(VENTANA_FIN))).thenReturn(9L);
        when(cliente.listarEventos(VENTANA_INICIO, VENTANA_FIN))
                .thenReturn(new SigaEventosRespuesta(3, 3, ""));

        var resultado = service.sincronizar();

        assertEquals("FALLIDA", resultado.estado());
        verify(registroSincronizacionPort).registrarFin(eq(9L), eq("FALLIDA"), eq(0),
                contains("no está confirmada"));
    }

    @Test
    void registraLaCorridaComoFallidaSiLaConsultaFalla() {
        when(registroSincronizacionPort.registrarInicio(any(String.class), any(), any())).thenReturn(9L);
        when(cliente.listarEventos(any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new IllegalStateException("tiempo de espera agotado"));

        var resultado = service.sincronizar();

        assertEquals("FALLIDA", resultado.estado());
        verify(registroSincronizacionPort).registrarFin(eq(9L), eq("FALLIDA"), eq(0),
                contains("tiempo de espera agotado"));
    }
}
