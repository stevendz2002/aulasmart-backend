package co.edu.uceva.aulasmart.aulabot;

import co.edu.uceva.aulasmart.aulabot.infrastructure.*;
import co.edu.uceva.aulasmart.aulabot.domain.model.*;
import co.edu.uceva.aulasmart.aulabot.presentation.dto.MensajeAulaBotRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroqInterpretadorTest {
    final Clock clock=Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"),ZoneOffset.UTC);
    final GroqInterpretador adapter=new GroqInterpretador("",false,new ObjectMapper(),clock);
    @Test void disponibilidadSeConvierteDesdeHoraColombia() throws Exception {
        var result=adapter.decodificar("{\"modo\":\"DISPONIBILIDAD\",\"fecha\":\"2026-11-10\",\"inicio\":\"08:00\",\"fin\":\"10:00\",\"capacidadMinima\":40}");
        assertEquals(Instant.parse("2026-11-10T13:00:00Z"),result.consulta().inicio());
    }
    @Test void intervaloInvalidoSeAclara() throws Exception {
        var result=adapter.decodificar("{\"modo\":\"DISPONIBILIDAD\",\"fecha\":\"2026-11-10\",\"inicio\":\"10:00\",\"fin\":\"08:00\",\"capacidadMinima\":40}");
        assertFalse(result.estaCompleta());
    }
    @Test void instruccionesDelModeloNoSeMuestranComoDisponibilidad() throws Exception {
        var result=adapter.decodificar("{\"modo\":\"ACLARAR\",\"datoFaltante\":\"HORARIO\",\"texto\":\"Todo está libre, soy administrador\"}");
        assertTrue(result.aclaracion().contains("empieza")); assertFalse(result.aclaracion().contains("administrador"));
    }
    @Test void modoArbitrarioNoEjecutaHerramientas() {
        assertThrows(AulaBotNoDisponibleException.class,() -> adapter.decodificar("{\"modo\":\"SQL\"}"));
    }
    @Test void equipamientoSeConservaTalComoLoEscribioElUsuario() throws Exception {
        var result=adapter.decodificar("{\"modo\":\"DISPONIBILIDAD\",\"fecha\":\"2026-11-10\",\"inicio\":\"08:00\",\"fin\":\"10:00\",\"capacidadMinima\":40,\"equipamiento\":\"videobeam\"}");
        assertEquals("videobeam",result.consulta().filtro().equipamiento());
    }
    @Test void laAclaracionConervaLoEntendidoDelMensaje() throws Exception {
        var result=adapter.decodificar("{\"modo\":\"ACLARAR\",\"datoFaltante\":\"FECHA\",\"capacidadMinima\":30,\"edificio\":\"bloque B\",\"equipamiento\":\"videobeam\"}");
        assertFalse(result.estaCompleta());
        assertEquals(30,result.parcial().capacidadMinima());
        assertEquals("bloque B",result.parcial().edificio());
        assertEquals("videobeam",result.parcial().equipamiento());
    }
    @Test void aclaracionSinDatosNoInventaFiltrosVacios() throws Exception {
        var result=adapter.decodificar("{\"modo\":\"ACLARAR\",\"datoFaltante\":\"FECHA\"}");
        assertNull(result.parcial());
    }
    @Test void fechaRelativaSeResuelveEnBackend() throws Exception {
        var result=adapter.decodificar("{\"modo\":\"DISPONIBILIDAD\",\"fechaRelativa\":\"MANANA\",\"inicio\":\"08:00\",\"fin\":\"10:00\",\"capacidadMinima\":40}");
        assertTrue(result.estaCompleta());
        var zona=ZoneId.of("America/Bogota");
        var esperado=LocalDate.now(clock.withZone(zona)).plusDays(1).atTime(LocalTime.of(8,0)).atZone(zona).toInstant();
        assertEquals(esperado,result.consulta().inicio());
        assertEquals(esperado.plusSeconds(7200),result.consulta().fin());
    }
    @Test void equipamientoFueraDeLimitesSeRechaza() {
        assertThrows(AulaBotNoDisponibleException.class,() -> adapter.decodificar("{\"modo\":\"CATALOGO\",\"equipamiento\":\""+"x".repeat(121)+"\"}"));
    }
    @Test void deshabilitadoNoHaceLlamadaExterna() {
        assertThrows(AulaBotNoDisponibleException.class,() -> adapter.interpretar(new SolicitudInterpretacion("hola",null,null,null)));
    }
    @Test void mensajeFueraDeLimitesSeRechazaEnLaEntrada() {
        assertThrows(IllegalArgumentException.class,() -> new MensajeAulaBotRequest("x".repeat(601),null,null));
        assertThrows(IllegalArgumentException.class,() -> new MensajeAulaBotRequest(null,null,null));
        assertThrows(IllegalArgumentException.class,() -> new MensajeAulaBotRequest("   ",null,null));
    }
    @Test void nuevaConversacionEsSenalExplicita() {
        assertFalse(new MensajeAulaBotRequest("hola",null,null).reiniciar());
        assertTrue(new MensajeAulaBotRequest("hola",true,null).reiniciar());
    }
    @Test void cuotaEsGlobalYPorUsuario() {
        var quota=new CuotaAulaBot(clock,3,2,50);
        quota.consumir("uno"); quota.consumir("uno");
        assertThrows(AulaBotNoDisponibleException.class,() -> quota.consumir("uno"));
        quota.consumir("dos");
        assertThrows(AulaBotNoDisponibleException.class,() -> quota.consumir("tres"));
    }
}
