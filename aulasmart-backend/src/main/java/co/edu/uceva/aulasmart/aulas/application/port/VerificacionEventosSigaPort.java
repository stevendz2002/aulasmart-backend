package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;

/**
 * Consulta en tiempo real (Just-In-Time) contra el API de SIGA para verificar
 * si un aula específica presenta eventos u ocupaciones académicas que colisionen
 * con el intervalo horario propuesto para una reserva.
 */
public interface VerificacionEventosSigaPort {

    /**
     * Devuelve true si SIGA reporta algún evento u ocupación académica en el
     * aula con código SIGA especificado durante el intervalo solicitado.
     */
    boolean aulaTieneEventosEnSiga(Long codigoSigaAula, Intervalo intervalo);
}

