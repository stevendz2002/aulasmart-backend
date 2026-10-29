package co.edu.uceva.aulasmart.aulas.application.port;

import java.time.LocalDate;

/**
 * Registros de control de las corridas de sincronización en la tabla
 * `sincronizacion_siga`. Cada corrida crea un registro `EN_CURSO` y lo
 * finaliza con `EXITOSA` o `FALLIDA` en el mismo registro.
 */
public interface RegistroSincronizacionPort {

    Long registrarInicio(String recurso, LocalDate ventanaInicio, LocalDate ventanaFin);

    void registrarFin(Long idSincronizacion, String estado, int registrosProcesados, String detalleError);
}
