package co.edu.uceva.aulasmart.integrations.siga.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.AulaConsultaPort;
import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.application.port.RegistroSincronizacionPort;
import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import co.edu.uceva.aulasmart.integrations.siga.application.model.ResultadoSincronizacion;
import co.edu.uceva.aulasmart.integrations.siga.infrastructure.http.SigaRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Corrida de sincronización de eventos: recorre las aulas del catálogo local,
 * consulta el calendario público de SIGA por cada una dentro de la ventana
 * `[hoy, hoy + diasVentana)` en hora de Colombia, y aplica la ocupación de
 * forma atómica en `horario_academico`. Toda la ventana se reemplaza en cada
 * corrida exitosa; las consultas HTTP fallidas abortan la corrida sin
 * comprometer la tabla, y el control queda registrado como FALLIDA.
 */
@Service
public class SincronizadorDeEventosService {

    public static final String RECURSO = "EVENTOS";
    /** Colombia mantiene offset fijo UTC-5 todo el año. */
    public static final ZoneOffset ZONA_COLOMBIA = ZoneOffset.ofHours(-5);
    public static final int DIAS_VENTANA_POR_OMISION = 7;

    private static final String ESTADO_EXITOSA = "EXITOSA";
    private static final String ESTADO_FALLIDA = "FALLIDA";
    private static final int LONGITUD_MAXIMA_DETALLE = 500;

    private final SigaRestClient cliente;
    private final AulaConsultaPort aulaConsultaPort;
    private final HorarioAcademicoPort horarioAcademicoPort;
    private final RegistroSincronizacionPort registroSincronizacionPort;
    private final Clock clock;
    private final int diasVentana;

    public SincronizadorDeEventosService(
            SigaRestClient cliente,
            AulaConsultaPort aulaConsultaPort,
            HorarioAcademicoPort horarioAcademicoPort,
            RegistroSincronizacionPort registroSincronizacionPort,
            Clock clock,
            @Value("${aulasmart.siga.eventos.dias-ventana:" + DIAS_VENTANA_POR_OMISION + "}") int diasVentana
    ) {
        if (diasVentana < 1 || diasVentana > 31) {
            throw new IllegalArgumentException("La ventana de eventos debe tener entre 1 y 31 días.");
        }
        this.cliente = cliente;
        this.aulaConsultaPort = aulaConsultaPort;
        this.horarioAcademicoPort = horarioAcademicoPort;
        this.registroSincronizacionPort = registroSincronizacionPort;
        this.clock = clock;
        this.diasVentana = diasVentana;
    }

    public ResultadoSincronizacion sincronizar() {
        Instant inicio = clock.instant();
        Long idRegistro = registroSincronizacionPort.registrarInicio(RECURSO);
        try {
            Ventana ventana = instantesVentana();
            List<EventoExterno> eventos = consultarEventos(ventana);
            int aplicadas = horarioAcademicoPort.aplicarEventosExternos(
                    eventos,
                    ventana.inicio(),
                    ventana.fin()
            );
            registroSincronizacionPort.registrarFin(idRegistro, ESTADO_EXITOSA, aplicadas, null);
            return resultado(inicio, ESTADO_EXITOSA, aplicadas, null);
        } catch (Exception e) {
            String detalle = truncar(e.toString());
            registroSincronizacionPort.registrarFin(idRegistro, ESTADO_FALLIDA, 0, detalle);
            return resultado(inicio, ESTADO_FALLIDA, 0, detalle);
        }
    }

    /** Consulta el calendario público aula por aula dentro de la ventana. */
    private List<EventoExterno> consultarEventos(Ventana ventana) {
        List<Long> codigos = aulaConsultaPort.buscarCodigosSigaDeAulasActivas();
        var eventos = new ArrayList<EventoExterno>();
        for (Long codigo : codigos) {
            eventos.addAll(cliente.listarEventos(codigo, ventana.fechaInicio(), ventana.fechaFin()));
        }
        return eventos;
    }

    /** La ventana se calcula desde el día actual en hora de Colombia. */
    private Ventana instantesVentana() {
        LocalDate hoy = LocalDate.ofInstant(clock.instant(), ZONA_COLOMBIA);
        LocalDate fin = hoy.plusDays(diasVentana);
        return new Ventana(
                hoy.atStartOfDay(ZONA_COLOMBIA).toInstant(),
                fin.atStartOfDay(ZONA_COLOMBIA).toInstant(),
                hoy,
                fin.minusDays(1)
        );
    }

    private ResultadoSincronizacion resultado(Instant inicio, String estado, int registros, String detalleError) {
        Instant finalizadaEn = clock.instant();
        return new ResultadoSincronizacion(
                RECURSO,
                estado,
                registros,
                inicio,
                finalizadaEn,
                Duration.between(inicio, finalizadaEn).toMillis(),
                detalleError
        );
    }

    private String truncar(String texto) {
        return texto.length() <= LONGITUD_MAXIMA_DETALLE
                ? texto
                : texto.substring(0, LONGITUD_MAXIMA_DETALLE - 3) + "...";
    }

    /** Rango de la corrida: fechas inclusivas y frontera instantánea exclusiva. */
    private record Ventana(
            Instant inicio,
            Instant fin,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
    }
}
