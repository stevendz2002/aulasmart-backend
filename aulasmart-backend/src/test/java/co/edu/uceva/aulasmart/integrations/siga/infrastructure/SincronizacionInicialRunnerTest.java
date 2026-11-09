package co.edu.uceva.aulasmart.integrations.siga.infrastructure;
import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.integrations.siga.application.service.SincronizacionSigaService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class SincronizacionInicialRunnerTest {
    private final SincronizacionSigaService servicio = mock(SincronizacionSigaService.class);
    private final SincronizacionSigaPort estado = mock(SincronizacionSigaPort.class);
    private final Instant ahora = Instant.parse("2026-11-09T03:00:00Z");
    private void ejecutar(boolean habilitado) {
        new SincronizacionInicialRunner(servicio, estado, Clock.fixed(ahora, ZoneOffset.UTC), Duration.ofHours(24), habilitado, 7)
                .run(new DefaultApplicationArguments());
    }
    @Test void deshabilitadoNoConsultaNiSincroniza() {
        ejecutar(false); verifyNoInteractions(servicio, estado);
    }
    @Test void omiteSoloConCoberturaVigenteEnColombia() {
        when(estado.catalogoAulasVigente(any())).thenReturn(true);
        when(estado.eventosCubren(any(), any())).thenReturn(true);
        ejecutar(true);
        verify(estado).eventosCubren(eq(ahora.minus(Duration.ofHours(24))), eq(new Intervalo(Instant.parse("2026-11-08T05:00:00Z"), Instant.parse("2026-11-15T05:00:00Z"))));
        verifyNoInteractions(servicio);
    }
    @Test void alRenovarCatalogoSiempreRenuevaEventosAunqueParecieranVigentes() {
        when(estado.eventosCubren(any(), any())).thenReturn(true);
        ejecutar(true);
        verify(servicio).sincronizar(List.of("AULAS", "EVENTOS"));
    }
    @Test void reparaEventosConCoberturaIncompletaOFalloReciente() {
        when(estado.catalogoAulasVigente(any())).thenReturn(true);
        ejecutar(true);
        verify(servicio).sincronizar(List.of("EVENTOS"));
    }
    @Test void falloNoImpideArranque() {
        when(servicio.sincronizar(anyList())).thenThrow(new RuntimeException("red"));
        assertDoesNotThrow(() -> ejecutar(true));
    }
}
