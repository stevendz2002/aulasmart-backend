package co.edu.uceva.aulasmart.integrations.siga.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.RegistroSincronizacionPort;
import co.edu.uceva.aulasmart.integrations.siga.SigaPropiedades;
import co.edu.uceva.aulasmart.integrations.siga.application.model.ResultadoSincronizacion;
import co.edu.uceva.aulasmart.integrations.siga.infrastructure.http.SigaRestClient;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Corrida de sincronización de eventos académicos. La ventana cubierta se
 * declara antes de consultar; si la respuesta trae eventos, la corrida queda
 * FALLIDA porque la estructura interna de un evento no está confirmada y no
 * se pueden interpretar sin inventar atributos.
 */
@Service
public class SincronizadorDeEventosService {

    public static final String RECURSO = "EVENTOS";
    private static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Bogota");
    private static final String ESTADO_EXITOSA = "EXITOSA";
    private static final String ESTADO_FALLIDA = "FALLIDA";
    private static final int LONGITUD_MAXIMA_DETALLE = 500;

    private final SigaRestClient cliente;
    private final RegistroSincronizacionPort registroSincronizacionPort;
    private final SigaPropiedades propiedades;
    private final Clock clock;

    public SincronizadorDeEventosService(
            SigaRestClient cliente,
            RegistroSincronizacionPort registroSincronizacionPort,
            SigaPropiedades propiedades,
            Clock clock
    ) {
        this.cliente = cliente;
        this.registroSincronizacionPort = registroSincronizacionPort;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public ResultadoSincronizacion sincronizar() {
        Instant inicio = clock.instant();
        LocalDate hoy = LocalDate.ofInstant(inicio, ZONA_INSTITUCIONAL);
        LocalDate desde = hoy.minusDays(propiedades.eventos().diasHaciaAtras());
        LocalDate hasta = hoy.plusDays(propiedades.eventos().diasHaciaAdelante());
        Long idRegistro = registroSincronizacionPort.registrarInicio(RECURSO, desde, hasta);
        try {
            var respuesta = cliente.listarEventos(desde, hasta);
            if (respuesta.cantidad() == 0) {
                registroSincronizacionPort.registrarFin(idRegistro, ESTADO_EXITOSA, 0, null);
                return resultado(inicio, ESTADO_EXITOSA, 0, null);
            }
            String detalle = "No se interpretaron " + respuesta.cantidad() + " eventos: la estructura interna de un "
                    + "evento todavía no está confirmada en las fuentes, así que la corrida se registra FALLIDA "
                    + "en lugar de inventar los campos.";
            registroSincronizacionPort.registrarFin(idRegistro, ESTADO_FALLIDA, 0, detalle);
            return resultado(inicio, ESTADO_FALLIDA, 0, detalle);
        } catch (Exception e) {
            String detalle = truncar(e.toString());
            registroSincronizacionPort.registrarFin(idRegistro, ESTADO_FALLIDA, 0, detalle);
            return resultado(inicio, ESTADO_FALLIDA, 0, detalle);
        }
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
}
