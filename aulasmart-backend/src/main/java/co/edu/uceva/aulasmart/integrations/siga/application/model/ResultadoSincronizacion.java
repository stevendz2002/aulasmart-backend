package co.edu.uceva.aulasmart.integrations.siga.application.model;

import java.time.Instant;

/**
 * Resultado de una corrida de sincronización sobre un recurso. Refleja lo
 * registrado en `sincronizacion_siga` y sirve como respuesta de la ejecución
 * manual y de la programada.
 */
public record ResultadoSincronizacion(
        String recurso,
        String estado,
        int registrosProcesados,
        Instant iniciadaEn,
        Instant finalizadaEn,
        long duracionMilisegundos,
        String detalleError
) {
}
