package co.edu.uceva.aulasmart.integrations.siga.infrastructure;

import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.integrations.siga.application.service.SincronizacionSigaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SincronizacionInicialRunnerTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final Duration MAX_ANTIGUEDAD = Duration.ofHours(24);

    @Mock
    private SincronizacionSigaService sincronizacionService;

    @Mock
    private SincronizacionSigaPort sincronizacionSigaPort;

    private Clock clock;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    }

    @Test
    void noSincronizaSiEstaDeshabilitado() {
        var runner = new SincronizacionInicialRunner(
                sincronizacionService,
                sincronizacionSigaPort,
                clock,
                MAX_ANTIGUEDAD,
                false
        );

        runner.run(new DefaultApplicationArguments());

        verifyNoInteractions(sincronizacionService);
        verifyNoInteractions(sincronizacionSigaPort);
    }

    @Test
    void noSincronizaSiTodoEstaVigente() {
        when(sincronizacionSigaPort.catalogoAulasVigente(any())).thenReturn(true);
        when(sincronizacionSigaPort.eventosVigente(any())).thenReturn(true);

        var runner = new SincronizacionInicialRunner(
                sincronizacionService,
                sincronizacionSigaPort,
                clock,
                MAX_ANTIGUEDAD,
                true
        );

        runner.run(new DefaultApplicationArguments());

        verifyNoInteractions(sincronizacionService);
    }

    @Test
    void sincronizaAulasYEventosSiNingunoEstaVigente() {
        when(sincronizacionSigaPort.catalogoAulasVigente(any())).thenReturn(false);
        when(sincronizacionSigaPort.eventosVigente(any())).thenReturn(false);

        var runner = new SincronizacionInicialRunner(
                sincronizacionService,
                sincronizacionSigaPort,
                clock,
                MAX_ANTIGUEDAD,
                true
        );

        runner.run(new DefaultApplicationArguments());

        verify(sincronizacionService).ejecutar("AULAS");
        verify(sincronizacionService).ejecutar("EVENTOS");
    }

    @Test
    void continuaConEventosSiFallaAulas() {
        when(sincronizacionSigaPort.catalogoAulasVigente(any())).thenReturn(false);
        when(sincronizacionSigaPort.eventosVigente(any())).thenReturn(false);
        when(sincronizacionService.ejecutar("AULAS")).thenThrow(new RuntimeException("Error simulado de red"));

        var runner = new SincronizacionInicialRunner(
                sincronizacionService,
                sincronizacionSigaPort,
                clock,
                MAX_ANTIGUEDAD,
                true
        );

        runner.run(new DefaultApplicationArguments());

        verify(sincronizacionService).ejecutar("AULAS");
        verify(sincronizacionService).ejecutar("EVENTOS");
    }
}

