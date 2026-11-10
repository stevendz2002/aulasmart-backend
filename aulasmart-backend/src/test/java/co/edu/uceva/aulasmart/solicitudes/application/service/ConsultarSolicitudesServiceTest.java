package co.edu.uceva.aulasmart.solicitudes.application.service;
import co.edu.uceva.aulasmart.solicitudes.application.port.SolicitudReservaPort;
import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ConsultarSolicitudesServiceTest {
    @Test void limitaHistorialSinOcultarActivasNiConsultarOtrosDocentes() {
        var port = mock(SolicitudReservaPort.class);
        var ahora = Instant.parse("2026-11-10T15:00:00Z");
        var lista = new ArrayList<SolicitudReserva>();
        for (long id=1;id<=55;id++) lista.add(new SolicitudReserva(id,1L,7L,
                new Intervalo(ahora.minusSeconds(7200),ahora.minusSeconds(3600)),
                "PENDIENTE","Clase",null,null,ahora.minusSeconds(10000)));
        lista.add(new SolicitudReserva(100L,1L,7L,new Intervalo(ahora.plusSeconds(3600),ahora.plusSeconds(7200)),
                "PENDIENTE","Clase",null,null,ahora));
        when(port.buscarPorDocente(7L)).thenReturn(lista);
        var service = new ConsultarSolicitudesService(port, Clock.fixed(ahora,ZoneOffset.UTC));
        var resultado = service.consultar(new PrincipalDeSolicitudes.UsuarioActivo(7L,"DOCENTE"),null);
        assertEquals(51,resultado.size());
        assertEquals(100L,resultado.get(0).id());
        assertEquals(50,resultado.stream().filter(s -> s.estado().equals("RECHAZADA")).count());
        assertTrue(resultado.stream().noneMatch(s -> s.id() <= 5));
        verify(port,never()).buscarTodas();
    }
}
