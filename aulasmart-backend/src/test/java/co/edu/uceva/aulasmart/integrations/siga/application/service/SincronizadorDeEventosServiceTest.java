package co.edu.uceva.aulasmart.integrations.siga.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.application.port.RegistroSincronizacionPort;
import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import co.edu.uceva.aulasmart.integrations.siga.application.model.ResultadoSincronizacion;
import co.edu.uceva.aulasmart.integrations.siga.infrastructure.http.SigaRestClient;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizadorDeEventosServiceTest {

    /**
     * 13:00Z de un lunes == 08:00 en Colombia: la ventana arranca ese día
     * (cero hora local = 05:00Z) y cubre 7 días corridos.
     */
    private static final Instant AHORA = Instant.parse("2026-11-02T13:00:00Z");
    private static final Instant INICIO_VENTANA = Instant.parse("2026-11-02T05:00:00Z");
    private static final Instant FIN_VENTANA = Instant.parse("2026-11-09T05:00:00Z");

    private static final EventoExterno CLASE =
            new EventoExterno(195L, "161510|2026-11-03 07:20:00",
                    Instant.parse("2026-11-03T12:20:00Z"), Instant.parse("2026-11-03T14:00:00Z"),
                    "Asignatura: 22034-220 - COSTOS Y PRESUPUESTOS");

    @Mock
    private SigaRestClient cliente;
    @Mock
    private AulaConsultaPort aulaConsultaPort;
    @Mock
    private HorarioAcademicoPort horarioAcademicoPort;
    @Mock
    private RegistroSincronizacionPort registroSincronizacionPort;

    private SincronizadorDeEventosService service;

    @BeforeEach
    void configurar() {
        service = new SincronizadorDeEventosService(
                cliente,
                aulaConsultaPort,
                horarioAcademicoPort,
                registroSincronizacionPort,
                Clock.fixed(AHORA, ZoneOffset.UTC),
                7
        );
    }

    @Test
    void consultaAulaPorAulaYAplicaLaVentanaCompleta() {
        when(registroSincronizacionPort.registrarInicio(SincronizadorDeEventosService.RECURSO)).thenReturn(7L);
        when(aulaConsultaPort.buscarCodigosSigaDeAulasActivas()).thenReturn(List.of(195L, 196L));
        when(cliente.listarEventos(195L, LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-08")))
                .thenReturn(List.of(CLASE));
        when(cliente.listarEventos(196L, LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-08")))
                .thenReturn(List.of());
        when(horarioAcademicoPort.aplicarEventosExternos(List.of(CLASE), INICIO_VENTANA, FIN_VENTANA))
                .thenReturn(1);

        ResultadoSincronizacion resultado = service.sincronizar();

        assertEquals(SincronizadorDeEventosService.RECURSO, resultado.recurso());
        assertEquals("EXITOSA", resultado.estado());
        assertEquals(1, resultado.registrosProcesados());
        verify(registroSincronizacionPort).registrarFin(7L, "EXITOSA", 1, null);
        verify(registroSincronizacionPort).registrarCobertura(7L, INICIO_VENTANA, FIN_VENTANA);
    }

    @Test
    void registraFallaSinAplicarNadaCuandoUnaConsultaDeAulaFalla() {
        when(registroSincronizacionPort.registrarInicio(SincronizadorDeEventosService.RECURSO)).thenReturn(8L);
        when(aulaConsultaPort.buscarCodigosSigaDeAulasActivas()).thenReturn(List.of(195L, 196L));
        when(cliente.listarEventos(195L, LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-08")))
                .thenReturn(List.of(CLASE));
        when(cliente.listarEventos(196L, LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-08")))
                .thenThrow(new IllegalStateException("La respuesta del endpoint de eventos no es JSON válido."));

        ResultadoSincronizacion resultado = service.sincronizar();

        assertEquals("FALLIDA", resultado.estado());
        assertEquals(0, resultado.registrosProcesados());
        verify(horarioAcademicoPort, never()).aplicarEventosExternos(anyList(), any(Instant.class), any(Instant.class));
        verify(registroSincronizacionPort).registrarFin(
                eq(8L),
                eq("FALLIDA"),
                eq(0),
                any(String.class)
        );
    }

    @Test
    void laVentanaRespetaLaCantidadDeDiasConfigurada() {
        var serviceTresDias = new SincronizadorDeEventosService(
                cliente,
                aulaConsultaPort,
                horarioAcademicoPort,
                registroSincronizacionPort,
                Clock.fixed(AHORA, ZoneOffset.UTC),
                3
        );
        when(registroSincronizacionPort.registrarInicio(any(String.class))).thenReturn(9L);
        when(aulaConsultaPort.buscarCodigosSigaDeAulasActivas()).thenReturn(List.of(195L));
        when(cliente.listarEventos(195L, LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-04")))
                .thenReturn(List.of());
        when(horarioAcademicoPort.aplicarEventosExternos(
                eq(List.of()), eq(INICIO_VENTANA), eq(Instant.parse("2026-11-05T05:00:00Z"))
        )).thenReturn(0);

        ResultadoSincronizacion resultado = serviceTresDias.sincronizar();

        assertEquals("EXITOSA", resultado.estado());
        assertEquals(Set.of(SincronizadorDeEventosService.RECURSO), Set.of(resultado.recurso()));
    }

    @Test
    void rechazaVentanasPorFueraDelRangoSano() {
        assertThrows(IllegalArgumentException.class, () -> new SincronizadorDeEventosService(
                cliente, aulaConsultaPort, horarioAcademicoPort, registroSincronizacionPort,
                Clock.fixed(AHORA, ZoneOffset.UTC), 0
        ));
        assertThrows(IllegalArgumentException.class, () -> new SincronizadorDeEventosService(
                cliente, aulaConsultaPort, horarioAcademicoPort, registroSincronizacionPort,
                Clock.fixed(AHORA, ZoneOffset.UTC), 32
        ));
    }
}
