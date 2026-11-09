package co.edu.uceva.aulasmart.integrations.siga.application.service;

import co.edu.uceva.aulasmart.aulas.application.port.CatalogoSigaPort;
import co.edu.uceva.aulasmart.aulas.application.port.RegistroSincronizacionPort;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaExterna;
import co.edu.uceva.aulasmart.integrations.siga.application.model.ResultadoSincronizacion;
import co.edu.uceva.aulasmart.integrations.siga.application.port.ConsultaSigaPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Corrida de sincronización del catálogo de aulas: consulta SIGA, valida la
 * respuesta, aplica el catálogo de forma atómica y registra el control
 * `EN_CURSO` → `EXITOSA`/`FALLIDA` en el mismo registro del control.
 */
@Service
public class SincronizadorDeAulasService {

    public static final String RECURSO = "AULAS";
    private static final String ESTADO_EXITOSA = "EXITOSA";
    private static final String ESTADO_FALLIDA = "FALLIDA";
    private static final int LONGITUD_MAXIMA_DETALLE = 500;

    private final ConsultaSigaPort cliente;
    private final CatalogoSigaPort catalogoSigaPort;
    private final RegistroSincronizacionPort registroSincronizacionPort;
    private final Clock clock;

    public SincronizadorDeAulasService(
            ConsultaSigaPort cliente,
            CatalogoSigaPort catalogoSigaPort,
            RegistroSincronizacionPort registroSincronizacionPort,
            Clock clock
    ) {
        this.cliente = cliente;
        this.catalogoSigaPort = catalogoSigaPort;
        this.registroSincronizacionPort = registroSincronizacionPort;
        this.clock = clock;
    }

    public ResultadoSincronizacion sincronizar() {
        Instant inicio = clock.instant();
        Long idRegistro = registroSincronizacionPort.registrarInicio(RECURSO);
        try {
            List<AulaExterna> aulas = cliente.listarAulas();
            validarCodigosUnicos(aulas);
            int aplicadas = catalogoSigaPort.aplicarAulasExternas(aulas);
            registroSincronizacionPort.registrarFin(idRegistro, ESTADO_EXITOSA, aplicadas, null);
            return resultado(inicio, ESTADO_EXITOSA, aplicadas, null);
        } catch (Exception e) {
            String detalle = truncar(e.toString());
            registroSincronizacionPort.registrarFin(idRegistro, ESTADO_FALLIDA, 0, detalle);
            return resultado(inicio, ESTADO_FALLIDA, 0, detalle);
        }
    }

    private void validarCodigosUnicos(List<AulaExterna> aulas) {
        long codigosDistintos = aulas.stream().map(AulaExterna::codigoAula).distinct().count();
        if (codigosDistintos != aulas.size()) {
            throw new IllegalStateException("La respuesta de aulas contiene códigos `codigo_aula` repetidos: "
                    + aulas.size() + " registros con " + codigosDistintos + " códigos distintos.");
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
