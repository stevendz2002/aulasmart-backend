package co.edu.uceva.aulasmart.solicitudes.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.solicitudes.application.port.SolicitudReservaPort;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.ReservaSolapadaException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudNoEncontradaException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudYaRevisadaException;
import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisarSolicitudServiceTest {
    private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");
    private static final Instant CREADA = Instant.parse("2026-10-29T14:00:00Z");
    private static final Long REVISOR = 9002L;
    private static final Long SOLICITUD = 70L;
    private static final Long AULA = 9002L;
    private static final Intervalo INTERVALO = new Intervalo(
            Instant.parse("2026-11-10T13:00:00Z"),
            Instant.parse("2026-11-10T15:00:00Z")
    );

    @Mock
    private SolicitudReservaPort solicitudReservaPort;
    @Mock
    private SolicitudAprobadaPort solicitudAprobadaPort;

    private RevisarSolicitudService service;

    @BeforeEach
    void configurar() {
        service = new RevisarSolicitudService(
                solicitudReservaPort,
                solicitudAprobadaPort,
                Clock.fixed(AHORA, ZoneOffset.UTC)
        );
    }

    @Test
    void apruebaSolicitudPendienteDejandoAuditoria() {
        when(solicitudReservaPort.buscarPorId(SOLICITUD)).thenReturn(Optional.of(pendiente()));
        when(solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(INTERVALO)).thenReturn(Set.of());
        when(solicitudReservaPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = service.revisar(SOLICITUD, REVISOR, "aprobar");

        assertEquals(SolicitudReserva.APROBADA, resultado.estado());
        assertEquals(REVISOR, resultado.revisadaPorId());
        assertEquals(AHORA, resultado.revisadaEn());
    }

    @Test
    void rechazaSolicitudPendienteDejandoAuditoria() {
        when(solicitudReservaPort.buscarPorId(SOLICITUD)).thenReturn(Optional.of(pendiente()));
        when(solicitudReservaPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = service.revisar(SOLICITUD, REVISOR, "RECHAZAR");

        assertEquals(SolicitudReserva.RECHAZADA, resultado.estado());
        assertEquals(REVISOR, resultado.revisadaPorId());
        verifyNoInteractions(solicitudAprobadaPort);
    }

    @Test
    void rechazaDecisionDesconocida() {
        var exception = assertThrows(IllegalArgumentException.class, () ->
                service.revisar(SOLICITUD, REVISOR, "CANCELAR"));

        assertEquals("La decisión de revisión debe ser APROBAR o RECHAZAR; el valor recibido fue `CANCELAR`.",
                exception.getMessage());
        verifyNoInteractions(solicitudReservaPort, solicitudAprobadaPort);
    }

    @Test
    void noRevisaSolicitudInexistente() {
        when(solicitudReservaPort.buscarPorId(SOLICITUD)).thenReturn(Optional.empty());

        var exception = assertThrows(SolicitudNoEncontradaException.class, () ->
                service.revisar(SOLICITUD, REVISOR, "APROBAR"));

        assertEquals("No existe una solicitud con el identificador " + SOLICITUD + ".", exception.getMessage());
        verifyNoInteractions(solicitudAprobadaPort);
    }

    @Test
    void noRevisaUnaSolicitudYaRevisada() {
        when(solicitudReservaPort.buscarPorId(SOLICITUD)).thenReturn(Optional.of(revisada(
                SolicitudReserva.RECHAZADA)));

        var exception = assertThrows(SolicitudYaRevisadaException.class, () ->
                service.revisar(SOLICITUD, REVISOR, "APROBAR"));

        assertEquals("La solicitud " + SOLICITUD + " ya fue revisada (estado RECHAZADA).", exception.getMessage());
        verifyNoInteractions(solicitudAprobadaPort);
        verify(solicitudReservaPort, never()).guardar(any());
    }

    @Test
    void noApruebaSiExisteReservaAprobadaSolapada() {
        when(solicitudReservaPort.buscarPorId(SOLICITUD)).thenReturn(Optional.of(pendiente()));
        when(solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(INTERVALO)).thenReturn(Set.of(AULA));

        var exception = assertThrows(ReservaSolapadaException.class, () ->
                service.revisar(SOLICITUD, REVISOR, "APROBAR"));

        assertEquals("El aula " + AULA + " ya tiene una reserva aprobada que se solapa con el intervalo solicitado.",
                exception.getMessage());
        verify(solicitudReservaPort, never()).guardar(any());
    }

    private SolicitudReserva pendiente() {
        return new SolicitudReserva(
                SOLICITUD,
                AULA,
                9001L,
                INTERVALO,
                SolicitudReserva.PENDIENTE,
                "PRUEBA: motivo de la solicitud.",
                null,
                null,
                CREADA
        );
    }

    private SolicitudReserva revisada(String estado) {
        return new SolicitudReserva(
                SOLICITUD,
                AULA,
                9001L,
                INTERVALO,
                estado,
                "PRUEBA: motivo de la solicitud.",
                REVISOR,
                AHORA,
                CREADA
        );
    }
}
