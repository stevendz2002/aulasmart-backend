package co.edu.uceva.aulasmart.aulabot.application.service;

import co.edu.uceva.aulasmart.aulabot.application.port.InterpretacionLenguajePort;
import co.edu.uceva.aulasmart.aulabot.application.port.RegistroConversacionesPort;
import co.edu.uceva.aulasmart.aulabot.domain.model.*;
import co.edu.uceva.aulasmart.aulas.application.service.*;
import co.edu.uceva.aulasmart.aulas.domain.model.*;
import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * Motor determinista del chat informativo: mantiene solo el contexto
 * estructurado vigente, combina los parámetros que el extractor de lenguaje
 * entrega, pregunta únicamente los datos faltantes y consulta disponibilidad
 * con los casos de uso reales. Las respuestas salen de plantillas del backend;
 * las acciones estructuradas no consumen llamadas al modelo.
 */
@Service
public class AulaBotService {
    private static final List<String> SUGERENCIAS = List.of("CAMBIAR_BLOQUE","CAMBIAR_HORARIO","VER_OPCIONES");
    private static final java.time.ZoneId ZONA = java.time.ZoneId.of("America/Bogota");
    private final InterpretacionLenguajePort language;
    private final ConsultarAulasService catalog;
    private final DisponibilidadAulaService availability;
    private final RegistroConversacionesPort conversations;
    private final Clock clock;
    public AulaBotService(InterpretacionLenguajePort language, ConsultarAulasService catalog,
            DisponibilidadAulaService availability, RegistroConversacionesPort conversations, Clock clock) {
        this.language=language; this.catalog=catalog; this.availability=availability;
        this.conversations=conversations; this.clock=clock;
    }
    public record Resultado(String texto,String estado,List<Aula> aulas,Instant inicio,Instant fin,
                            Instant verificadoEn,FiltroAulaBot filtros,int total,List<String> sugerencias) {}

    public Resultado enviar(String usuario, boolean reiniciar, String mensaje) {
        if (usuario==null || usuario.isBlank()) throw new IllegalArgumentException("Se necesita la identidad del usuario.");
        var estadoPrevio=conversations.estado(usuario,reiniciar);
        var interpreted=conversations.repeticionReciente(usuario,mensaje,estadoPrevio)
                .orElseGet(() -> language.interpretar(new SolicitudInterpretacion(mensaje,
                        estadoPrevio.consultaVigente(),estadoPrevio.aclaracionPendiente(),estadoPrevio.franjaPendiente())));
        conversations.registrar(usuario,mensaje,estadoPrevio,interpreted);
        if(!interpreted.estaCompleta()) {
            var vigenteNueva=mezclar(estadoPrevio.consultaVigente(),interpreted.parcial());
            var franjaNueva=combinarFranja(estadoPrevio.franjaPendiente(),interpreted.franja());
            if (franjaNueva!=null && franjaNueva.completa()) {
                // la franja se completó por turnos: se consulta disponibilidad real con los filtros vigentes
                var filtro=vigenteNueva==null?new FiltroAulaBot(null,null,null,null,null,null):vigenteNueva.filtro();
                var consulta=new ConsultaAulaBot(filtro,
                        franjaNueva.fecha().atTime(franjaNueva.inicio()).atZone(ZONA).toInstant(),
                        franjaNueva.fecha().atTime(franjaNueva.fin()).atZone(ZONA).toInstant(),"");
                var result=consultar(consulta);
                var estadoNueva=new EstadoConversacionAulaBot(consulta,null,null,clock.instant());
                conversations.guardar(usuario,estadoNueva);
                conversations.registrar(usuario,mensaje,estadoNueva,interpreted);
                return result;
            }
            var aclaracion=(franjaNueva==null && vigenteNueva==null)
                    ?interpreted.aclaracion():preguntaFaltante(franjaNueva,interpreted.aclaracion());
            var estadoNueva=new EstadoConversacionAulaBot(vigenteNueva,aclaracion,franjaNueva,clock.instant());
            conversations.guardar(usuario,estadoNueva);
            conversations.registrar(usuario,mensaje,estadoNueva,interpreted);
            return empty(aclaracion,"ACLARACION",vigenteNueva,SUGERENCIAS);
        }
        var result=consultar(interpreted.consulta());
        var pendiente="ACLARACION".equals(result.estado())?result.texto():null;
        var estadoNueva=new EstadoConversacionAulaBot(interpreted.consulta(),pendiente,null,clock.instant());
        conversations.guardar(usuario,estadoNueva);
        conversations.registrar(usuario,mensaje,estadoNueva,interpreted);
        return result;
    }

    /** Acciones estructuradas de los chips: no llaman al modelo de lenguaje. */
    public Resultado accionar(String usuario, String accion) {
        if (usuario==null || usuario.isBlank()) throw new IllegalArgumentException("Se necesita la identidad del usuario.");
        var vigente=conversations.estado(usuario,false).consultaVigente();
        return switch (AccionAulaBot.desde(accion)) {
            case BUSCAR_AULA -> {
                if (vigente==null) {
                    var pregunta="¿Qué aula o filtros necesitas? Indica bloque, capacidad o equipamiento.";
                    guardar(usuario,null,pregunta);
                    yield empty(pregunta,"ACLARACION",null,List.of("CONSULTAR_DISPONIBILIDAD","VER_OPCIONES"));
                }
                yield consultar(vigente);
            }
            case CONSULTAR_DISPONIBILIDAD -> {
                if (vigente==null || vigente.inicio()==null) {
                    var pregunta="Indica fecha y horario para consultar disponibilidad (por ejemplo: mañana de 08:00 a 10:00).";
                    guardar(usuario,vigente,pregunta);
                    yield empty(pregunta,"ACLARACION",vigente,SUGERENCIAS);
                }
                yield consultar(vigente);
            }
            case CAMBIAR_BLOQUE -> {
                var nueva=vigente==null?null:new ConsultaAulaBot(new FiltroAulaBot(
                        vigente.filtro().capacidadMinima(),null,vigente.filtro().tipoAula(),vigente.filtro().dependencia(),
                        vigente.filtro().nombre(),vigente.filtro().equipamiento()),vigente.inicio(),vigente.fin(),"");
                var pregunta="¿En qué bloque la necesitas?";
                guardar(usuario,nueva,pregunta);
                yield empty(pregunta,"ACLARACION",nueva,SUGERENCIAS);
            }
            case CAMBIAR_HORARIO -> {
                var nueva=vigente==null?null:new ConsultaAulaBot(vigente.filtro(),null,null,"");
                var pregunta="¿En qué horario la necesitas?";
                guardar(usuario,nueva,pregunta);
                yield empty(pregunta,"ACLARACION",nueva,SUGERENCIAS);
            }
            case VER_OPCIONES -> {
                if (vigente==null) {
                    var pregunta="Indica qué aula o filtros quieres ver.";
                    guardar(usuario,null,pregunta);
                    yield empty(pregunta,"ACLARACION",null,List.of("BUSCAR_AULA"));
                }
                var nueva=new ConsultaAulaBot(vigente.filtro(),null,null,"");
                guardar(usuario,nueva,null);
                yield consultar(nueva);
            }
        };
    }

    private void guardar(String usuario, ConsultaAulaBot vigente, String pendiente) {
        conversations.guardar(usuario,new EstadoConversacionAulaBot(vigente,pendiente,null,clock.instant()));
    }

    private static FranjaAulaBot combinarFranja(FranjaAulaBot previa, FranjaAulaBot nueva) {
        if (nueva==null) return previa;
        if (previa==null) return nueva.vacia()?null:nueva;
        return new FranjaAulaBot(
                nueva.fecha()!=null?nueva.fecha():previa.fecha(),
                nueva.inicio()!=null?nueva.inicio():previa.inicio(),
                nueva.fin()!=null?nueva.fin():previa.fin());
    }
    private static String preguntaFaltante(FranjaAulaBot franja, String aclaracionModelo) {
        if (franja==null || franja.fecha()==null) return "¿Para qué fecha necesitas el aula?";
        if (franja.inicio()==null || franja.fin()==null)
            return "¿A qué hora empieza y termina? Indica ambas horas, por ejemplo de 08:00 a 10:00.";
        return aclaracionModelo;
    }

    /** Mezcla conservadora: el filtro parcial nuevo reemplaza campo por campo lo vigente. */
    private static ConsultaAulaBot mezclar(ConsultaAulaBot vigente, FiltroAulaBot parcial) {
        if (parcial==null) return vigente;
        var base=vigente==null?null:vigente.filtro();
        var filtro=base==null?parcial:new FiltroAulaBot(
                parcial.capacidadMinima()!=null?parcial.capacidadMinima():base.capacidadMinima(),
                parcial.edificio()!=null?parcial.edificio():base.edificio(),
                parcial.tipoAula()!=null?parcial.tipoAula():base.tipoAula(),
                parcial.dependencia()!=null?parcial.dependencia():base.dependencia(),
                parcial.nombre()!=null?parcial.nombre():base.nombre(),
                parcial.equipamiento()!=null?parcial.equipamiento():base.equipamiento());
        if (filtro.equals(base) && vigente!=null) return vigente;
        return new ConsultaAulaBot(filtro,vigente==null?null:vigente.inicio(),vigente==null?null:vigente.fin(),"");
    }

    private Resultado consultar(ConsultaAulaBot query) {
        var filter=query.filtro();
        var all=catalog.consultar(new AulaFiltro(null,null,null,null));
        try {
            var building=resolve(all,Aula::edificio,filter.edificio());
            var type=resolve(all,Aula::tipoAula,filter.tipoAula());
            var department=resolve(all,Aula::dependencia,filter.dependencia());
            var actualFilter=new AulaFiltro(building==null?null:building.id(),type==null?null:type.id(),
                    department==null?null:department.id(),filter.capacidadMinima());
            List<Aula> rooms; Instant verified=null;
            if(query.inicio()!=null) {
                var result=availability.consultar(actualFilter,new Intervalo(query.inicio(),query.fin()));
                rooms=result.aulas(); verified=result.verificadoEn();
            } else rooms=catalog.consultar(actualFilter);
            if(filter.nombre()!=null) {
                String name=normalize(filter.nombre());
                var exact=rooms.stream().filter(a -> normalize(a.nombre()).equals(name) || String.valueOf(a.codigoSiga()).equals(name)).toList();
                rooms=exact.isEmpty()?rooms.stream().filter(a -> normalize(a.nombre()).contains(name)).toList():exact;
            }
            if(filter.equipamiento()!=null) {
                var grupo=EquipamientoAula.desde(filter.equipamiento()).orElseThrow(
                        () -> new FiltroAmbiguo(equipamientoDesconocido(filter.equipamiento(),EquipamientoAula.observados(all))));
                rooms=rooms.stream().filter(room -> grupo.presente(normalize(room.nombre()))).toList();
            }
            if(!rooms.isEmpty()) {
                String texto=rooms.size()>10?detalle(query)+" Muestro las primeras 10; precisa los filtros para reducir la lista."
                        :detalle(query);
                return new Resultado(texto,query.inicio()==null?"CATALOGO":"DISPONIBILIDAD",rooms.stream().limit(10).toList(),
                        query.inicio(),query.fin(),verified,filter,rooms.size(),SUGERENCIAS);
            }
            boolean conFranja=query.inicio()!=null;
            return new Resultado("No encontré aulas "+(conFranja
                    ?"libres con esos datos en la franja consultada"
                    :"con esos datos en el catálogo")
                    +". Amplía la capacidad, prueba otra franja o revisa la Consulta de aulas.",
                    "SIN_RESULTADOS",List.of(),query.inicio(),query.fin(),null,filter,0,SUGERENCIAS);
        } catch (FiltroAmbiguo e) { return empty(e.getMessage(),"ACLARACION",query,SUGERENCIAS); }
        catch (DisponibilidadNoVerificableException e) {
            return new Resultado("No puedo verificar esa franja con los datos actuales de SIGA. Puedes intentar otra fecha u horario, o abrir la Consulta de aulas.",
                    "NO_VERIFICABLE",List.of(),query.inicio(),query.fin(),null,filter,0,SUGERENCIAS);
        }
    }
    private static String detalle(ConsultaAulaBot query) {
        return query.inicio()==null
                ?"Estas aulas coinciden con el catálogo; indica fecha y horario para revisar su disponibilidad."
                :"Estas aulas están libres en la franja consultada. Para solicitar una reserva usa la sección Solicitudes; el chat no tramita solicitudes.";
    }
    private static Resultado empty(String text,String state,ConsultaAulaBot query,List<String> sugerencias) {
        return new Resultado(text,state,List.of(),query==null?null:query.inicio(),query==null?null:query.fin(),null,
                query==null?null:query.filtro(),0,sugerencias);
    }
    private static CatalogoAula resolve(List<Aula> rooms,Function<Aula,CatalogoAula> field,String value) {
        if(value==null) return null;
        var candidates=rooms.stream().map(field).filter(Objects::nonNull).distinct().toList();
        var exact=candidates.stream().filter(c -> normalize(c.nombre()).equals(normalize(value))).toList();
        var matches=exact.isEmpty()?candidates.stream().filter(c -> normalize(c.nombre()).contains(normalize(value))).toList():exact;
        if(matches.size()!=1) throw new FiltroAmbiguo(matches.isEmpty()?"No reconozco «"+value+"» en el catálogo. Indica el nombre del edificio, tipo o dependencia, o búscalo en la Consulta de aulas.":
                "Hay varias coincidencias para «"+value+"». Precisa una: "+String.join(", ",matches.stream().limit(6).map(CatalogoAula::nombre).toList()));
        return matches.get(0);
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();
    }
    private static String equipamientoDesconocido(String palabra,java.util.Set<String> observados) {
        if(observados.isEmpty()) return "El catálogo actual no evidencia equipamiento en los nombres de las aulas. Busca sin ese requisito o indica el aula exacta.";
        return "No evidencio «"+palabra+"» en el catálogo. El equipamiento que aparece en los nombres de las aulas es: "
                +String.join(", ",observados)+". ¿Con cuál de esos te ayudo, o busco sin ese requisito?";
    }
    private static class FiltroAmbiguo extends RuntimeException { FiltroAmbiguo(String message) { super(message); } }
}
