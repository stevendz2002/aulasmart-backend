package co.edu.uceva.aulasmart.aulabot.domain.model;

/**
 * Entrada para interpretar un mensaje: el texto libre y el contexto
 * estructurado vigente de la conversación. El proveedor de lenguaje solo
 * extrae parámetros; el estado lo decide el backend.
 */
public record SolicitudInterpretacion(String mensaje, ConsultaAulaBot consultaVigente, String aclaracionPendiente,
        FranjaAulaBot franjaPendiente) {
    public SolicitudInterpretacion {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje a interpretar es obligatorio.");
        }
        mensaje = mensaje.strip();
        if (aclaracionPendiente != null && aclaracionPendiente.isBlank()) {
            aclaracionPendiente = null;
        }
    }
}
