package co.edu.uceva.aulasmart.solicitudes.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.domain.exception.AulaNoEncontradaException;
import co.edu.uceva.aulasmart.aulas.domain.model.Aula;
import co.edu.uceva.aulasmart.aulas.domain.model.CatalogoAula;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.solicitudes.application.port.SolicitudReservaPort;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.ReservaSolapadaException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarSolicitudServiceTest {
    private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");
    private static final Long DOCENTE = 9001L;
    private static final Long AULA = 9002L;
    private static final Intervalo INTERVALO = new Intervalo(
            Instant.parse("2026-11-10T13:00:00Z"),
            Instant.parse("2026-11-10T15:00:00Z")
    );

    @Mock
    private SolicitudReservaPort solicitudReservaPort;
    @Mock
    private AulaConsultaPort aulaConsultaPort;
    @Mock
    private SolicitudAprobadaPort solicitudAprobadaPort;

    private RegistrarSolicitudService service;

    @BeforeEach
    void configurar() {
        service = new RegistrarSolicitudService(
                solicitudReservaPort,
                aulaConsultaPort,
                solicitudAprobadaPort,
                Clock.fixed(AHORA, ZoneOffset.UTC)
        );
    }

    @Test
    void registraSolicitudPendienteConMotivoNormalizado() {
        when(solicitudReservaPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(aulaConsultaPort.buscarActivaPorId(AULA)).thenReturn(Optional.of(aula()));

        var solicitud = service.registrar(DOCENTE, AULA, INTERVALO.inicio(), INTERVALO.fin(), "  Proyecto de clase  ");

        assertNull(solicitud.id());
        assertEquals(AULA, solicitud.aulaId());
        assertEquals(DOCENTE, solicitud.docenteId());
        assertEquals(SolicitudReserva.PENDIENTE, solicitud.estado());
        assertEquals("Proyecto de clase", solicitud.motivo());
        assertNull(solicitud.revisadaPorId());
        assertEquals(AHORA, solicitud.creadaEn());
        verify(solicitudReservaPort).guardar(solicitud);
    }

    @Test
    void rechazaMotivoAusente() {
        var exception = assertThrows(IllegalArgumentException.class, () ->
                service.registrar(DOCENTE, AULA, INTERVALO.inicio(), INTERVALO.fin(), null));

        assertEquals("El motivo de la solicitud es obligatorio.", exception.getMessage());
        verifyNoInteractions(solicitudReservaPort, aulaConsultaPort);
    }

    @Test
    void rechazaMotivoEnBlanco() {
        var exception = assertThrows(IllegalArgumentException.class, () ->
                service.registrar(DOCENTE, AULA, INTERVALO.inicio(), INTERVALO.fin(), "   "));

        assertEquals("El motivo de la solicitud es obligatorio.", exception.getMessage());
        verifyNoInteractions(solicitudReservaPort, aulaConsultaPort);
    }

    @Test
    void rechazaAulaInexistenteOInactiva() {
        when(aulaConsultaPort.buscarActivaPorId(AULA)).thenReturn(Optional.empty());

        assertThrows(AulaNoEncontradaException.class, () ->
                service.registrar(DOCENTE, AULA, INTERVALO.inicio(), INTERVALO.fin(), "Clase"));

        verifyNoInteractions(solicitudReservaPort, solicitudAprobadaPort);
    }

    @Test
    void rechazaIntervaloSolapadoConReservaAprobada() {
        when(aulaConsultaPort.buscarActivaPorId(AULA)).thenReturn(Optional.of(aula()));
        when(solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(INTERVALO)).thenReturn(Set.of(AULA));

        var exception = assertThrows(ReservaSolapadaException.class, () ->
                service.registrar(DOCENTE, AULA, INTERVALO.inicio(), INTERVALO.fin(), "Clase"));

        assertEquals("El aula " + AULA + " ya tiene una reserva aprobada que se solapa con el intervalo solicitado.",
                exception.getMessage());
        verifyNoInteractions(solicitudReservaPort);
    }

    private Aula aula() {
        // El caso de uso solo usa el identificador del aula; los demás campos son irrelevantes aquí.
        var catalogo = new CatalogoAula(1L, -1L, "PRUEBA");
        return new Aula(AULA, -AULA, "PRUEBA Aula 102", catalogo, 30, catalogo, catalogo, true);
    }
}
