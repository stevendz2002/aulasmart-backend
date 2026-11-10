package co.edu.uceva.aulasmart.aulabot.presentation.dto;

/**
 * Entrada del chat: lenguaje natural o una acción estructurada de un chip.
 * La señal de nueva conversación y los permisos se resuelven en el servidor.
 */
public record MensajeAulaBotRequest(String mensaje, Boolean nuevaConversacion, String accion) {
    public MensajeAulaBotRequest {
        if (nuevaConversacion == null) {
            nuevaConversacion = false;
        }
        if (accion != null) {
            accion = accion.strip();
            if (accion.isBlank() || accion.length() > 40) {
                throw new IllegalArgumentException("Acción inválida.");
            }
        } else {
            if (mensaje == null || mensaje.isBlank()) {
                throw new IllegalArgumentException("El mensaje es obligatorio.");
            }
            if (mensaje.strip().length() > 600) {
                throw new IllegalArgumentException("El mensaje no puede superar 600 caracteres.");
            }
            mensaje = mensaje.strip();
        }
    }

    public boolean reiniciar() {
        return Boolean.TRUE.equals(nuevaConversacion);
    }
}
