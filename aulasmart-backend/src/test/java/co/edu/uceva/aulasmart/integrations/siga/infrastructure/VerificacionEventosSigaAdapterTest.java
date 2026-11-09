package co.edu.uceva.aulasmart.integrations.siga.infrastructure;

import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.integrations.siga.infrastructure.http.SigaRestClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificacionEventosSigaAdapterTest {
    private final SigaRestClient cliente = mock(SigaRestClient.class);
    private final VerificacionEventosSigaAdapter adapter = new VerificacionEventosSigaAdapter(cliente);
    private final LocalDate fecha = LocalDate.of(2026, 11, 10);
    private final Intervalo intervalo = new Intervalo(
            Instant.parse("2026-11-10T13:00:00Z"), Instant.parse("2026-11-10T14:00:00Z"));

    @Test
    void fallaCerradoCuandoSigaNoResponde() {
        when(cliente.listarEventos(123L, fecha, fecha)).thenThrow(new IllegalStateException("Fallo simulado"));
        assertThrows(DisponibilidadNoVerificableException.class,
                () -> adapter.aulaTieneEventosEnSiga(123L, intervalo));
    }

    @Test
    void respuestaNulaNoConfirmaDisponibilidad() {
        when(cliente.listarEventos(123L, fecha, fecha)).thenReturn(null);
        assertThrows(DisponibilidadNoVerificableException.class,
                () -> adapter.aulaTieneEventosEnSiga(123L, intervalo));
    }

    @Test
    void listaVaciaConfirmaAusenciaDeEventos() {
        when(cliente.listarEventos(123L, fecha, fecha)).thenReturn(List.of());
        assertFalse(adapter.aulaTieneEventosEnSiga(123L, intervalo));
    }

    @Test
    void detectaSolapePeroPermiteIntervalosAdyacentes() {
        when(cliente.listarEventos(123L, fecha, fecha)).thenReturn(List.of(
                new EventoExterno(123L, "1", intervalo.inicio(), intervalo.fin(), "Clase")));
        assertTrue(adapter.aulaTieneEventosEnSiga(123L, intervalo));
        when(cliente.listarEventos(123L, fecha, fecha)).thenReturn(List.of(
                new EventoExterno(123L, "2", intervalo.fin(), intervalo.fin().plusSeconds(3600), "Clase")));
        assertFalse(adapter.aulaTieneEventosEnSiga(123L, intervalo));
    }

    @Test
    void conservaAulasSinteticasSinConsultarSiga() {
        assertFalse(adapter.aulaTieneEventosEnSiga(-1L, intervalo));
        verifyNoInteractions(cliente);
    }

    @Test
    void codigoAusenteNoConfirmaDisponibilidad() {
        assertThrows(DisponibilidadNoVerificableException.class, () -> adapter.aulaTieneEventosEnSiga(null, intervalo));
        assertThrows(DisponibilidadNoVerificableException.class, () -> adapter.aulaTieneEventosEnSiga(0L, intervalo));
    }
}
