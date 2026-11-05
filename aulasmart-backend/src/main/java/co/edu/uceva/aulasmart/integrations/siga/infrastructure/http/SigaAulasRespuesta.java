package co.edu.uceva.aulasmart.integrations.siga.infrastructure.http;

import co.edu.uceva.aulasmart.aulas.domain.model.AulaExterna;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpreta la respuesta del endpoint público de aulas. Las verificaciones
 * corresponden a la evidencia documentada: la raíz contiene `success`, `data`
 * y `total`; en las peticiones observaradas el endpoint no respondió a la
 * paginación solicitada, por lo que la cantidad se comprueba explícitamente
 * y los objetos deben traer los nueve campos estructurados.
 */
public record SigaAulasRespuesta(int total, List<AulaExterna> aulas) {

    public static SigaAulasRespuesta desde(ObjectMapper objectMapper, String cuerpo) {
        JsonNode raiz;
        try {
            raiz = objectMapper.readTree(cuerpo);
        } catch (IOException e) {
            throw new SigaRespuestaInvalidaException("La respuesta del endpoint de aulas no es JSON válido.", e);
        }
        if (!raiz.path("success").asBoolean(false)) {
            throw new SigaRespuestaInvalidaException("El endpoint de aulas devolvió success=false.");
        }
        JsonNode datos = raiz.get("data");
        if (datos == null || !datos.isArray()) {
            throw new SigaRespuestaInvalidaException("El endpoint de aulas no devolvió un arreglo `data`.");
        }
        JsonNode total = raiz.get("total");
        if (total == null || !total.isInt()) {
            throw new SigaRespuestaInvalidaException("El endpoint de aulas no devolvió un `total` numérico.");
        }
        if (total.asInt() != datos.size()) {
            throw new SigaRespuestaInvalidaException(
                    "El total declarado (" + total.asInt() + ") no coincide con la cantidad recibida (" + datos.size()
                            + "). El endpoint no respondió a la paginación solicitada; se rechaza de forma conservadora.");
        }
        var aulas = new ArrayList<AulaExterna>(datos.size());
        for (int indice = 0; indice < datos.size(); indice++) {
            aulas.add(extraerAula(datos.get(indice), indice));
        }
        return new SigaAulasRespuesta(total.asInt(), List.copyOf(aulas));
    }

    private static AulaExterna extraerAula(JsonNode nodo, int indice) {
        return new AulaExterna(
                numero(nodo, "codigo_aula", indice),
                texto(nodo, "nombre_aula", indice),
                numero(nodo, "codigo_edificio", indice),
                texto(nodo, "nombre_edificio", indice),
                sillas(nodo, indice),
                numero(nodo, "codigo_dependencia", indice),
                texto(nodo, "nombre_dependencia", indice),
                numero(nodo, "codigo_tipo_aula", indice),
                texto(nodo, "nombre_tipo_aula", indice)
        );
    }

    private static Long numero(JsonNode nodo, String campo, int indice) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.canConvertToLong()) {
            throw new SigaRespuestaInvalidaException(
                    "El aula en el índice " + indice + " no incluye `" + campo + "` como número.");
        }
        return valor.asLong();
    }

    private static int sillas(JsonNode nodo, int indice) {
        JsonNode valor = nodo.get("sillas");
        if (valor == null || !valor.isInt() || valor.asInt() < 0) {
            throw new SigaRespuestaInvalidaException(
                    "El aula en el índice " + indice + " no incluye `sillas` como entero no negativo.");
        }
        return valor.asInt();
    }

    private static String texto(JsonNode nodo, String campo, int indice) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.isTextual() || valor.asText().isBlank()) {
            throw new SigaRespuestaInvalidaException(
                    "El aula en el índice " + indice + " no incluye `" + campo + "` como texto no vacío.");
        }
        return valor.asText().trim();
    }
}
