package co.edu.uceva.aulasmart.integrations.siga.presentation;

import co.edu.uceva.aulasmart.integrations.siga.application.model.ResultadoSincronizacion;

import java.time.Instant;

/**
 * Resumen de una corrida de sincronización.
 */
public record SincronizacionResponse(
        String recurso,
        String estado,
        int registrosProcesados,
        Instant iniciadaEn,
        Instant finalizadaEn,
        long duracionMilisegundos,
        String detalleError
) {

    static SincronizacionResponse desde(ResultadoSincronizacion resultado) {
        return new SincronizacionResponse(
                resultado.recurso(),
                resultado.estado(),
                resultado.registrosProcesados(),
                resultado.iniciadaEn(),
                resultado.finalizadaEn(),
                resultado.duracionMilisegundos(),
                resultado.detalleError()
        );
    }
}
