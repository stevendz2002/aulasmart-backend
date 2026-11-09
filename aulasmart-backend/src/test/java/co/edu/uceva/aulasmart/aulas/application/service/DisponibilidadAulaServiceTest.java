package co.edu.uceva.aulasmart.aulas.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.application.port.SincronizacionSigaPort;
import co.edu.uceva.aulasmart.aulas.application.port.SolicitudAprobadaPort;
import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import co.edu.uceva.aulasmart.aulas.domain.model.Aula;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaFiltro;
import co.edu.uceva.aulasmart.aulas.domain.model.CatalogoAula;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadAulaServiceTest {
    private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");
    private static final Intervalo INTERVALO = new Intervalo(
            Instant.parse("2026-11-10T13:00:00Z"),
            Instant.parse("2026-11-10T15:00:00Z")
    );
    private static final AulaFiltro FILTRO = new AulaFiltro(null, null, null, 30);
    private static final Instant LIMITE = AHORA.minus(Duration.ofHours(24));

    @Mock
    private AulaConsultaPort aulaConsultaPort;
    @Mock
    private SolicitudAprobadaPort solicitudAprobadaPort;
    @Mock
    private HorarioAcademicoPort horarioAcademicoPort;
    @Mock
    private SincronizacionSigaPort sincronizacionSigaPort;

    private DisponibilidadAulaService service;

    @BeforeEach
    void configurar() {
        service = new DisponibilidadAulaService(
                aulaConsultaPort,
                solicitudAprobadaPort,
                horarioAcademicoPort,
                sincronizacionSigaPort,
                Clock.fixed(AHORA, ZoneOffset.UTC),
                Duration.ofHours(24)
        );
    }

    @Test
    void excluyeLaUnionDeReservasAprobadasYEventosSiga() {
        var aulaUno = aula(1L, "PRUEBA Aula 1");
        var aulaDos = aula(2L, "PRUEBA Aula 2");
        var aulaTres = aula(3L, "PRUEBA Aula 3");
        var aulaCuatro = aula(4L, "PRUEBA Aula 4");
        when(sincronizacionSigaPort.catalogoAulasVigente(LIMITE)).thenReturn(true);
        when(sincronizacionSigaPort.eventosCubren(LIMITE, INTERVALO)).thenReturn(true);
        when(solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(INTERVALO)).thenReturn(Set.of(2L));
        when(horarioAcademicoPort.buscarAulasConEventos(INTERVALO)).thenReturn(Set.of(3L));
        when(aulaConsultaPort.buscarActivas(FILTRO)).thenReturn(List.of(aulaUno, aulaDos, aulaTres, aulaCuatro));

        var resultado = service.consultar(FILTRO, INTERVALO);

        assertEquals(List.of(aulaUno, aulaCuatro), resultado.aulas());
        assertEquals(AHORA, resultado.verificadoEn());
    }

    @Test
    void unaMismaAulaOcupadaPorReservaYEventoNoSeDuplicaNiPasaALibres() {
        var aulaUno = aula(1L, "PRUEBA Aula 1");
        when(sincronizacionSigaPort.catalogoAulasVigente(LIMITE)).thenReturn(true);
        when(sincronizacionSigaPort.eventosCubren(LIMITE, INTERVALO)).thenReturn(true);
        when(solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(INTERVALO)).thenReturn(Set.of(2L));
        when(horarioAcademicoPort.buscarAulasConEventos(INTERVALO)).thenReturn(Set.of(2L, 3L));
        when(aulaConsultaPort.buscarActivas(FILTRO)).thenReturn(List.of(aulaUno));

        var resultado = service.consultar(FILTRO, INTERVALO);

        assertEquals(List.of(aulaUno), resultado.aulas());
    }

    @Test
    void noAfirmaDisponibilidadConCatalogoVencido() {
        when(sincronizacionSigaPort.catalogoAulasVigente(LIMITE)).thenReturn(false);

        assertThrows(DisponibilidadNoVerificableException.class, () -> service.consultar(FILTRO, INTERVALO));

        verifyNoInteractions(solicitudAprobadaPort, horarioAcademicoPort, aulaConsultaPort);
    }

    @Test
    void noAfirmaDisponibilidadConEventosVencidos() {
        when(sincronizacionSigaPort.catalogoAulasVigente(LIMITE)).thenReturn(true);
        when(sincronizacionSigaPort.eventosCubren(LIMITE, INTERVALO)).thenReturn(false);

        assertThrows(DisponibilidadNoVerificableException.class, () -> service.consultar(FILTRO, INTERVALO));

        verifyNoInteractions(solicitudAprobadaPort, horarioAcademicoPort, aulaConsultaPort);
    }

    @Test
    void devuelveAulasActivasCuandoNoHayOcupacionAlguna() {
        var aula = aula(1L, "PRUEBA Aula 1");
        when(sincronizacionSigaPort.catalogoAulasVigente(LIMITE)).thenReturn(true);
        when(sincronizacionSigaPort.eventosCubren(LIMITE, INTERVALO)).thenReturn(true);
        when(solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(INTERVALO)).thenReturn(Set.of());
        when(horarioAcademicoPort.buscarAulasConEventos(INTERVALO)).thenReturn(Set.of());
        when(aulaConsultaPort.buscarActivas(FILTRO)).thenReturn(List.of(aula));

        var resultado = service.consultar(FILTRO, INTERVALO);

        assertEquals(List.of(aula), resultado.aulas());
        verify(sincronizacionSigaPort).eventosCubren(LIMITE, INTERVALO);
    }

    private Aula aula(Long id, String nombre) {
        var catalogo = new CatalogoAula(1L, -1L, "PRUEBA");
        return new Aula(id, -id, nombre, catalogo, 40, catalogo, catalogo, true);
    }

    @Test
    void detalleUneEventosYReservasSinExponerMotivosPrivados() {
        when(aulaConsultaPort.buscarActivaPorId(1L)).thenReturn(java.util.Optional.of(aula(1L, "Aula")));
        when(sincronizacionSigaPort.catalogoAulasVigente(LIMITE)).thenReturn(true);
        when(sincronizacionSigaPort.eventosCubren(LIMITE, INTERVALO)).thenReturn(true);
        var evento = new co.edu.uceva.aulasmart.aulas.domain.model.OcupacionAula(1L, "Clase", INTERVALO.inicio(), INTERVALO.fin());
        var reserva = new co.edu.uceva.aulasmart.aulas.domain.model.OcupacionAula(1L, "Solicitud aprobada en AulaSmart", INTERVALO.inicio(), INTERVALO.fin());
        when(horarioAcademicoPort.buscarOcupacionDeAula(1L, INTERVALO)).thenReturn(List.of(evento));
        when(solicitudAprobadaPort.buscarOcupacion(1L, INTERVALO)).thenReturn(List.of(reserva));
        assertEquals(List.of(evento, reserva), service.consultarOcupacion(1L, INTERVALO));
    }

    @Test
    void detalleNoPresentaVacioSiNoHayCobertura() {
        when(aulaConsultaPort.buscarActivaPorId(1L)).thenReturn(java.util.Optional.of(aula(1L, "Aula")));
        when(sincronizacionSigaPort.catalogoAulasVigente(LIMITE)).thenReturn(true);
        assertThrows(DisponibilidadNoVerificableException.class, () -> service.consultarOcupacion(1L, INTERVALO));
        verifyNoInteractions(horarioAcademicoPort, solicitudAprobadaPort);
    }
}
