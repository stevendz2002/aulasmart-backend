package co.edu.uceva.aulasmart.aulas.application.port;

import java.time.Instant;

/**
 * Registros de control de las corridas de sincronización en la tabla
 * `sincronizacion_siga`. Cada corrida crea un registro `EN_CURSO` y lo
 * finaliza con `EXITOSA` o `FALLIDA` en el mismo registro.
 */
public interface RegistroSincronizacionPort {
    void registrarCobertura(Long idSincronizacion, Instant inicio, Instant fin);

    Long registrarInicio(String recurso);

    void registrarFin(Long idSincronizacion, String estado, int registrosProcesados, String detalleError);
}
