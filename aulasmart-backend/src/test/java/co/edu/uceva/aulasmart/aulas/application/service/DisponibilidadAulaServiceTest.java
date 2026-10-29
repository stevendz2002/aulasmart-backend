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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

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

    @Mock
    private AulaConsultaPort aulaConsultaPort;
    @Mock
    private HorarioAcademicoPort horarioAcademicoPort;
    @Mock
    private SolicitudAprobadaPort solicitudAprobadaPort;
    @Mock
    private SincronizacionSigaPort sincronizacionSigaPort;

    private DisponibilidadAulaService service;

    @BeforeEach
    void configurar() {
        service = new DisponibilidadAulaService(
                aulaConsultaPort,
                horarioAcademicoPort,
                solicitudAprobadaPort,
                sincronizacionSigaPort,
                Clock.fixed(AHORA, ZoneOffset.UTC),
                Duration.ofHours(24)
        );
    }

    @Test
    void excluyeHorariosYSolicitudesAprobadasSolapados() {
        var aulaUno = aula(1L, "PRUEBA Aula 1");
        var aulaDos = aula(2L, "PRUEBA Aula 2");
        var aulaTres = aula(3L, "PRUEBA Aula 3");
        var limite = AHORA.minus(Duration.ofHours(24));
        when(sincronizacionSigaPort.datosCompletosYVigentes(INTERVALO, limite)).thenReturn(true);
        when(horarioAcademicoPort.buscarAulasOcupadas(INTERVALO)).thenReturn(Set.of(1L));
        when(solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(INTERVALO)).thenReturn(Set.of(2L));
        when(aulaConsultaPort.buscarActivas(FILTRO)).thenReturn(List.of(aulaUno, aulaDos, aulaTres));

        var resultado = service.consultar(FILTRO, INTERVALO);

        assertEquals(List.of(aulaTres), resultado.aulas());
        assertEquals(AHORA, resultado.verificadoEn());
        verify(sincronizacionSigaPort).datosCompletosYVigentes(INTERVALO, limite);
    }

    @Test
    void noAfirmaDisponibilidadConSincronizacionInvalida() {
        when(sincronizacionSigaPort.datosCompletosYVigentes(
                INTERVALO,
                AHORA.minus(Duration.ofHours(24))
        )).thenReturn(false);

        assertThrows(DisponibilidadNoVerificableException.class, () -> service.consultar(FILTRO, INTERVALO));

        verifyNoInteractions(horarioAcademicoPort, solicitudAprobadaPort, aulaConsultaPort);
    }

    @Test
    void devuelveAulasActivasCuandoNoExistenOcupaciones() {
        var aula = aula(1L, "PRUEBA Aula 1");
        when(sincronizacionSigaPort.datosCompletosYVigentes(
                INTERVALO,
                AHORA.minus(Duration.ofHours(24))
        )).thenReturn(true);
        when(horarioAcademicoPort.buscarAulasOcupadas(INTERVALO)).thenReturn(Set.of());
        when(solicitudAprobadaPort.buscarAulasConSolicitudesAprobadas(INTERVALO)).thenReturn(Set.of());
        when(aulaConsultaPort.buscarActivas(FILTRO)).thenReturn(List.of(aula));

        var resultado = service.consultar(FILTRO, INTERVALO);

        assertEquals(List.of(aula), resultado.aulas());
    }

    private Aula aula(Long id, String nombre) {
        var catalogo = new CatalogoAula(1L, -1L, "PRUEBA");
        return new Aula(id, -id, nombre, catalogo, 40, catalogo, catalogo, true);
    }
}
