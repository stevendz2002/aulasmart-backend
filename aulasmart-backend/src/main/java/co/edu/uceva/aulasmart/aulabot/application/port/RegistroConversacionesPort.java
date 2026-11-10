package co.edu.uceva.aulasmart.aulabot.application.port;

import co.edu.uceva.aulasmart.aulabot.domain.model.EstadoConversacionAulaBot;
import co.edu.uceva.aulasmart.aulabot.domain.model.InterpretacionAulaBot;

import java.util.Optional;

/**
 * Conversaciones separadas por usuario, en memoria y sin persistencia.
 * Guarda solo el contexto estructurado vigente y evita repetir una
 * interpretación idéntica dentro de una ventana corta. Nunca almacena
 * resultados de disponibilidad.
 */
public interface RegistroConversacionesPort {

    EstadoConversacionAulaBot estado(String usuario, boolean reiniciar);

    void guardar(String usuario, EstadoConversacionAulaBot estado);

    /** Interpretación idéntica ya calculada para el mismo mensaje y el mismo estado, vigente en la ventana corta. */
    Optional<InterpretacionAulaBot> repeticionReciente(String usuario, String mensaje, EstadoConversacionAulaBot estado);

    void registrar(String usuario, String mensaje, EstadoConversacionAulaBot estado, InterpretacionAulaBot interpretacion);
}
