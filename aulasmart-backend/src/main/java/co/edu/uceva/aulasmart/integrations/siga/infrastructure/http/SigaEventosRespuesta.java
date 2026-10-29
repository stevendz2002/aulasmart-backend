package co.edu.uceva.aulasmart.integrations.siga.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Interpreta la respuesta del endpoint público de eventos. La raíz contiene
 * `success`, `evts`, `message` y `total`. Todas las respuestas reales
 * documentadas tuvieron `evts` vacío; por tanto, si la lista trae elementos,
 * este registro solo informa la cantidad porque la estructura interna de un
 * evento no está confirmada y no se inventan campos.
 */
public record SigaEventosRespuesta(int total, int cantidad, String mensaje) {

    public static SigaEventosRespuesta desde(ObjectMapper objectMapper, String cuerpo) {
        JsonNode raiz;
        try {
            raiz = objectMapper.readTree(cuerpo);
        } catch (IOException e) {
            throw new SigaRespuestaInvalidaException("La respuesta del endpoint de eventos no es JSON válido.", e);
        }
        String mensaje = raiz.path("message").asText("");
        if (!raiz.path("success").asBoolean(false)) {
            throw new SigaRespuestaInvalidaException("El endpoint de eventos devolvió success=false"
                    + (mensaje.isBlank() ? "." : ": " + mensaje));
        }
        JsonNode eventos = raiz.get("evts");
        if (eventos == null || !eventos.isArray()) {
            throw new SigaRespuestaInvalidaException("El endpoint de eventos no devolvió un arreglo `evts`.");
        }
        JsonNode total = raiz.get("total");
        if (total == null || !total.isInt()) {
            throw new SigaRespuestaInvalidaException("El endpoint de eventos no devolvió un `total` numérico.");
        }
        if (total.asInt() != eventos.size()) {
            throw new SigaRespuestaInvalidaException(
                    "El total declarado (" + total.asInt() + ") no coincide con la cantidad recibida ("
                            + eventos.size() + "). El endpoint no respondió a la paginación solicitada; se rechaza de forma conservadora.");
        }
        return new SigaEventosRespuesta(total.asInt(), eventos.size(), mensaje);
    }
}
