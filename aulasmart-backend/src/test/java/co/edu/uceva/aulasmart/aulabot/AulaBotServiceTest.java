package co.edu.uceva.aulasmart.aulabot;

import co.edu.uceva.aulasmart.aulabot.application.port.InterpretacionLenguajePort;
import co.edu.uceva.aulasmart.aulabot.application.service.AulaBotService;
import co.edu.uceva.aulasmart.aulabot.domain.model.*;
import co.edu.uceva.aulasmart.aulabot.infrastructure.ConversacionesEnMemoriaAdapter;
import co.edu.uceva.aulasmart.aulas.application.service.*;
import co.edu.uceva.aulasmart.aulas.domain.model.*;
import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AulaBotServiceTest {
    final Clock clock=Clock.fixed(Instant.parse("2026-11-09T13:00:00Z"),ZoneOffset.UTC);
    final InterpretacionLenguajePort language=mock(InterpretacionLenguajePort.class);
    final ConsultarAulasService catalog=mock(ConsultarAulasService.class);
    final DisponibilidadAulaService availability=mock(DisponibilidadAulaService.class);
    final ConversacionesEnMemoriaAdapter conversations=new ConversacionesEnMemoriaAdapter(
            clock,Duration.ofMinutes(30),Duration.ofSeconds(90));
    final AulaBotService service=new AulaBotService(language,catalog,availability,conversations,clock);
    final CatalogoAula building=new CatalogoAula(1L,1L,"Bloque B - Avellanos");
    final Aula room=new Aula(9L,195L,"B101-PANTALLA",building,50,new CatalogoAula(2L,2L,"Aula audiovisual"),new CatalogoAula(3L,3L,"Informática"),true);
    final Instant start=Instant.parse("2030-01-10T13:00:00Z"),end=start.plusSeconds(7200);
    final List<SolicitudInterpretacion> recibidas=new ArrayList<>();
    final List<ConsultaAulaBot> respuestas=new ArrayList<>();

    void consulta(FiltroAulaBot filter,boolean timed) {
        var response=new ConsultaAulaBot(filter,timed?start:null,timed?end:null,"");
        doAnswer(inv -> {
            recibidas.add(inv.getArgument(0));
            respuestas.add(response);
            return InterpretacionAulaBot.completa(response);
        }).when(language).interpretar(any());
        when(catalog.consultar(any())).thenReturn(List.of(room));
    }

    @Test void consultaUsaFiltrosEIntervaloReales() {
        consulta(new FiltroAulaBot(40,"bloque b","audiovisual","informatica",null),true);
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start.minusSeconds(60),List.of(room)));
        var result=service.enviar("estudiante",false,"consulta de aula");
        verify(availability).consultar(new AulaFiltro(1L,2L,3L,40),new Intervalo(start,end));
        assertEquals(List.of(room),result.aulas()); assertEquals("DISPONIBILIDAD",result.estado());
        assertNotNull(result.verificadoEn());
        assertTrue(result.texto().contains("Solicitudes"));
    }

    @Test void mensajeRepetidoReutilizaLaInterpretacionSinGastarCuota() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),true);
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of(room)));
        service.enviar("estudiante",false,"aula para 40 en bloque B mañana de 8 a 10");
        var result=service.enviar("estudiante",false,"aula para 40 en bloque B mañana de 8 a 10");
        verify(language,times(1)).interpretar(any());
        verify(availability,times(2)).consultar(any(),any());
        assertEquals("DISPONIBILIDAD",result.estado());
    }

    @Test void repetirElMismoMensajeTrasCambiarElContextoSeInterpretaDeNuevo() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),false);
        service.enviar("estudiante",false,"aula para 40 en bloque B");
        service.enviar("estudiante",false,"aula para 40 en bloque B");
        verify(language,times(1)).interpretar(any());
        doAnswer(inv -> InterpretacionAulaBot.requiereAclaracion("¿Y el horario?"))
                .when(language).interpretar(any());
        service.enviar("estudiante",false,"hola");
        consulta(new FiltroAulaBot(40,"Bloque B",null),false);
        service.enviar("estudiante",false,"aula para 40 en bloque B");
        verify(language,times(3)).interpretar(any());
    }

    @Test void aclaracionConservaLoQueYaEntendioDelMensaje() {
        var parcial=new FiltroAulaBot(30,"bloque B",null,null,null,"videobeam");
        doAnswer(inv -> { recibidas.add(inv.getArgument(0));
            return InterpretacionAulaBot.requiereAclaracion("¿Para qué fecha necesitas el aula?",parcial); })
                .when(language).interpretar(any());
        var primero=service.enviar("estudiante",false,"aula para 30 en el bloque B con videobeam");
        assertEquals("ACLARACION",primero.estado());
        assertEquals(30,primero.filtros().capacidadMinima());
        assertEquals("bloque B",primero.filtros().edificio());
        assertEquals("videobeam",primero.filtros().equipamiento());
        var trasPrimerTurno=conversations.estado("estudiante",false);
        assertEquals("bloque B",trasPrimerTurno.consultaVigente().filtro().edificio());
        assertEquals("¿Para qué fecha necesitas el aula?",trasPrimerTurno.aclaracionPendiente());
        service.enviar("estudiante",false,"mañana de 8 a 10");
        assertEquals(2,recibidas.size());
        assertSame(trasPrimerTurno.consultaVigente(),recibidas.get(1).consultaVigente());
    }

    @Test void segundoMensajeConservaLaConsultaVigenteDelServidor() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),true);
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of(room)));
        service.enviar("estudiante",false,"aula para 40 en bloque B mañana de 8 a 10");
        var filtroSegundo=new FiltroAulaBot(50,"Bloque B",null);
        doAnswer(inv -> { recibidas.add(inv.getArgument(0)); return InterpretacionAulaBot.completa(new ConsultaAulaBot(filtroSegundo,start,end,"")); })
                .when(language).interpretar(any());
        var result=service.enviar("estudiante",false,"mejor para 50 personas");
        assertTrue(recibidas.size()>=2);
        assertSame(respuestas.get(0),recibidas.get(1).consultaVigente());
        verify(availability).consultar(new AulaFiltro(1L,null,null,50),new Intervalo(start,end));
        assertEquals("DISPONIBILIDAD",result.estado());
    }

    @Test void reiniciarLimpiaElContextoDelServidor() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),true);
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of(room)));
        service.enviar("estudiante",false,"aula para 40 en bloque B mañana de 8 a 10");
        service.enviar("estudiante",true,"otra consulta");
        assertEquals(2,recibidas.size());
        assertNull(recibidas.get(1).consultaVigente());
        assertNull(recibidas.get(1).aclaracionPendiente());
    }

    @Test void usuariosSeparadosNoCompartenMemoria() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),true);
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of(room)));
        service.enviar("estudiante",false,"aula para 40 en bloque B mañana de 8 a 10");
        consulta(new FiltroAulaBot(null,null,null),false);
        service.enviar("otro.usuario",false,"ayuda");
        assertTrue(recibidas.size()>=2);
        var ultima=recibidas.get(recibidas.size()-1);
        assertNull(ultima.consultaVigente());
    }

    @Test void accionCambiarBloqueNoLlamaAlModeloYConservaElResto() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),true);
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of(room)));
        service.enviar("estudiante",false,"aula para 40 en bloque B mañana de 8 a 10");
        reset(language);
        var result=service.accionar("estudiante","CAMBIAR_BLOQUE");
        verifyNoInteractions(language);
        assertEquals("ACLARACION",result.estado());
        assertNull(result.filtros().edificio());
        assertEquals(40,result.filtros().capacidadMinima());
        assertEquals(start,result.inicio());
        assertEquals("¿En qué bloque la necesitas?",result.texto());
    }

    @Test void accionCambiarHorarioLimpiaLaFranja() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),true);
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of(room)));
        service.enviar("estudiante",false,"aula para 40 en bloque B mañana de 8 a 10");
        reset(language);
        var result=service.accionar("estudiante","CAMBIAR_HORARIO");
        verifyNoInteractions(language);
        assertNull(result.inicio()); assertNull(result.fin());
        assertEquals("Bloque B",result.filtros().edificio());
        assertEquals("¿En qué horario la necesitas?",result.texto());
    }

    @Test void accionConsultarDisponibilidadSinFranjaPideFecha() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),false);
        service.enviar("estudiante",false,"aula para 40 en bloque B");
        reset(language);
        var result=service.accionar("estudiante","CONSULTAR_DISPONIBILIDAD");
        verifyNoInteractions(language);
        assertEquals("ACLARACION",result.estado());
        assertTrue(result.texto().contains("fecha y horario"));
        assertNotNull(conversations.estado("estudiante",false).aclaracionPendiente());
    }

    @Test void accionVerOpcionesConsultaCatalogoSinModelo() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),true);
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of(room)));
        service.enviar("estudiante",false,"aula para 40 en bloque B mañana de 8 a 10");
        reset(language); reset(availability);
        var result=service.accionar("estudiante","VER_OPCIONES");
        verifyNoInteractions(language);
        assertEquals("CATALOGO",result.estado());
        verifyNoInteractions(availability);
    }

    @Test void sigaNoVerificableNuncaSeTransformaEnLibre() {
        consulta(new FiltroAulaBot(null,null,null),true);
        when(availability.consultar(any(),any())).thenThrow(new DisponibilidadNoVerificableException());
        var result=service.enviar("estudiante",false,"consulta de franja");
        assertEquals("NO_VERIFICABLE",result.estado()); assertTrue(result.aulas().isEmpty());
        assertTrue(result.texto().contains("No puedo verificar"));
    }

    @Test void sinResultadosEsInformativo() {
        consulta(new FiltroAulaBot(40,"Bloque B",null),true);
        when(catalog.consultar(any())).thenReturn(List.of(room));
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of()));
        var result=service.enviar("estudiante",false,"bloque b de 8 a 10");
        assertEquals("SIN_RESULTADOS",result.estado());
        assertTrue(result.texto().contains("No encontré aulas"));
    }

    @Test void edificioDesconocidoNoSeIgnora() {
        consulta(new FiltroAulaBot(null,"Edificio inexistente",null),false);
        assertEquals("ACLARACION",service.enviar("estudiante",false,"busca en ese bloque").estado());
        verifyNoInteractions(availability);
    }

    @Test void edificioAmbiguoPideElegir() {
        consulta(new FiltroAulaBot(null,"Bloque",null),false);
        var other=new Aula(10L,196L,"A101",new CatalogoAula(8L,8L,"Bloque A"),40,room.tipoAula(),room.dependencia(),true);
        when(catalog.consultar(any())).thenReturn(List.of(room,other));
        assertEquals("ACLARACION",service.enviar("estudiante",false,"busca por bloque").estado());
    }

    @Test void equipamientoFiltraConLosNombresRealesDelCatalogo() {
        var llana=new Aula(10L,196L,"A102",building,30,room.tipoAula(),room.dependencia(),true);
        when(catalog.consultar(any())).thenReturn(List.of(room,llana));
        doAnswer(inv -> { recibidas.add(inv.getArgument(0));
            return InterpretacionAulaBot.completa(new ConsultaAulaBot(new FiltroAulaBot(null,null,null,null,null,"videobeam"),null,null,"")); })
                .when(language).interpretar(any());
        var result=service.enviar("estudiante",false,"aula con videobeam");
        assertEquals(1,result.total());
        assertEquals("B101-PANTALLA",result.aulas().get(0).nombre());
        verifyNoInteractions(availability);
    }

    @Test void terminoDesconocidoSeAclaraConElCatalogoReal() {
        var llana=new Aula(10L,196L,"A102",building,30,room.tipoAula(),room.dependencia(),true);
        when(catalog.consultar(any())).thenReturn(List.of(room,llana));
        doAnswer(inv -> { recibidas.add(inv.getArgument(0));
            return InterpretacionAulaBot.completa(new ConsultaAulaBot(new FiltroAulaBot(null,null,null,null,null,"tablero"),null,null,"")); })
                .when(language).interpretar(any());
        var result=service.enviar("estudiante",false,"aula con tablero");
        assertEquals("ACLARACION",result.estado());
        assertTrue(result.texto().contains("pantalla"));
        verifyNoInteractions(availability);
    }

    @Test void fechaYHorarioEnTurnosDistintosCompletanLaBusqueda() {
        var parcial=new FiltroAulaBot(30,"bloque B",null,null,null,"videobeam");
        var manana=java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")).plusDays(1);
        doAnswer(inv -> { recibidas.add(inv.getArgument(0));
            return InterpretacionAulaBot.requiereAclaracion("¿Para qué fecha necesitas el aula?",parcial); })
                .when(language).interpretar(any());
        service.enviar("estudiante",false,"aula para 30 en el bloque B con videobeam");
        doAnswer(inv -> { recibidas.add(inv.getArgument(0));
            return InterpretacionAulaBot.requiereAclaracion("¿A qué hora empieza y termina?",null,
                    new FranjaAulaBot(manana,null,null)); })
                .when(language).interpretar(any());
        service.enviar("estudiante",false,"mañana");
        when(availability.consultar(any(),any())).thenReturn(new Disponibilidad(new Intervalo(start,end),start,List.of(room)));
        when(catalog.consultar(any())).thenReturn(List.of(room));
        doAnswer(inv -> { recibidas.add(inv.getArgument(0));
            return InterpretacionAulaBot.requiereAclaracion("Completando",null,
                    new FranjaAulaBot(null,java.time.LocalTime.of(9,0),java.time.LocalTime.of(11,0))); })
                .when(language).interpretar(any());
        var result=service.enviar("estudiante",false,"de 9 a 11");
        assertEquals("DISPONIBILIDAD",result.estado());
        assertEquals(List.of(room),result.aulas());
        verify(availability).consultar(any(),any());
    }

    @Test void exigeIdentidadDeUsuario() {
        assertThrows(IllegalArgumentException.class,() -> service.enviar(" ",false,"hola"));
    }
}
