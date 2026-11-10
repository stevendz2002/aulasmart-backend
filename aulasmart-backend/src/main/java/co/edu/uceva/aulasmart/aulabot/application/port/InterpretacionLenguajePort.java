package co.edu.uceva.aulasmart.aulabot.application.port;

import co.edu.uceva.aulasmart.aulabot.domain.model.InterpretacionAulaBot;
import co.edu.uceva.aulasmart.aulabot.domain.model.SolicitudInterpretacion;

/**
 * Interpreta lenguaje natural sin conceder acceso a datos ni permisos. La
 * implementación puede ser un proveedor externo; la salida siempre se valida
 * antes de usar los casos de uso reales. Recibe el estado vigente de la
 * conversación; no guarda memoria ni identidad.
 */
public interface InterpretacionLenguajePort {

    InterpretacionAulaBot interpretar(SolicitudInterpretacion solicitud);
}
