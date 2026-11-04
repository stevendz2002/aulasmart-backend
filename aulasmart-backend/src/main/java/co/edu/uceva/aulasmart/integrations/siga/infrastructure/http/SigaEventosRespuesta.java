package co.edu.uceva.aulasmart.integrations.siga.infrastructure.http;

import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpreta la respuesta del endpoint público `listarEventAulaPublico`
 * (verificado sobre el módulo `publicomanejoespacios` de SIGA). La raíz
 * contiene `success`, `evts` y `total`; cada evento trae `id_unico`,
 * `evt_id`, `code_location` (== `codigo_aula`) y las fechas `start_dt` /
 * `end_dt` en hora local de Colombia (UTC-5 fijo, sin horario de verano).
 * Cuando se omite el aula o la fecha, el endpoint responde
 * `evts` vacío con el mensaje "Parametros invalidos": se rechaza de forma
 * conservadora porque no es posible afirmar que no haya ocupación.
 */
public record SigaEventosRespuesta(int total, List<EventoExterno> eventos) {

    /** Colombia mantiene UTC-5 todo el año (offset fijo, sin DST). */
    static final ZoneOffset ZONA_COLOMBIA = ZoneOffset.ofHours(-5);

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int LONGITUD_MAXIMA_ID_EXTERNO = 120;

    public static SigaEventosRespuesta desde(ObjectMapper objectMapper, String cuerpo) {
        JsonNode raiz;
        try {
            raiz = objectMapper.readTree(cuerpo);
        } catch (IOException e) {
            throw new SigaRespuestaInvalidaException("La respuesta del endpoint de eventos no es JSON válido.", e);
        }
        if (!raiz.path("success").asBoolean(false)) {
            throw new SigaRespuestaInvalidaException("El endpoint de eventos devolvió success=false.");
        }
        JsonNode eventos = raiz.get("evts");
        if (eventos == null || !eventos.isArray()) {
            throw new SigaRespuestaInvalidaException("El endpoint de eventos no devolvió un arreglo `evts`.");
        }
        JsonNode total = raiz.get("total");
        if (total == null || !total.isInt()) {
            throw new SigaRespuestaInvalidaException("El endpoint de eventos no devolvió un `total` numérico.");
        }
        if (eventos.isEmpty() && !raiz.path("message").asText("").isBlank()) {
            throw new SigaRespuestaInvalidaException(
                    "El endpoint de eventos rechazó la petición: " + raiz.path("message").asText());
        }
        if (total.asInt() != eventos.size()) {
            throw new SigaRespuestaInvalidaException(
                    "El total declarado (" + total.asInt() + ") no coincide con los eventos recibidos ("
                            + eventos.size() + ").");
        }
        var ocurrencias = new ArrayList<EventoExterno>(eventos.size());
        for (int indice = 0; indice < eventos.size(); indice++) {
            EventoExterno evento = extraerEvento(eventos.get(indice), indice);
            if (evento != null) {
                ocurrencias.add(evento);
            }
        }
        return new SigaEventosRespuesta(total.asInt(), List.copyOf(ocurrencias));
    }

    private static EventoExterno extraerEvento(JsonNode nodo, int indice) {
        if (nodo.path("cancelar").asBoolean(false)) {
            return null;
        }

        Instant inicio = fechaHora(nodo, "start_dt", indice);
        Instant fin = fechaHora(nodo, "end_dt", indice);
        String identificador = identificadorExterno(nodo, inicio, indice);
        return new EventoExterno(numero(nodo, "code_location", indice), identificador, inicio, fin);
    }

    /**
     * El índice externo único de una ocurrencia: el identificador del evento
     * del calendario SIGA unido a su fecha de inicio. `evt_id` se repite entre
     * las ocurrencias semanales de un mismo horario (verificado), por eso la
     * fecha de inicio es necesaria para la unicidad por ocurrencia.
     */
    private static String identificadorExterno(JsonNode nodo, Instant inicio, int indice) {
        JsonNode evtId = nodo.get("evt_id");
        if (evtId == null || !evtId.canConvertToLong()) {
            throw new SigaRespuestaInvalidaException(
                    "El evento en el índice " + indice + " no incluye `evt_id` como número.");
        }
        String inicioLocal = LocalDateTime.ofInstant(inicio, ZONA_COLOMBIA)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String idExterno = evtId.asLong() + "|" + inicioLocal;
        if (idExterno.length() > LONGITUD_MAXIMA_ID_EXTERNO) {
            throw new SigaRespuestaInvalidaException(
                    "El identificador externo del evento en el índice " + indice + " excede 120 caracteres.");
        }
        return idExterno;
    }

    private static Instant fechaHora(JsonNode nodo, String campo, int indice) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.isTextual()) {
            throw new SigaRespuestaInvalidaException(
                    "El evento en el índice " + indice + " no incluye `" + campo + "` como texto.");
        }
        try {
            return LocalDateTime.parse(valor.asText(), FECHA_HORA).toInstant(ZONA_COLOMBIA);
        } catch (DateTimeParseException e) {
            throw new SigaRespuestaInvalidaException(
                    "El evento en el índice " + indice + " tiene `" + campo + "` fuera del formato esperado "
                            + "(yyyy-MM-dd HH:mm:ss en hora de Colombia).",
                    e);
        }
    }

    private static Long numero(JsonNode nodo, String campo, int indice) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.canConvertToLong()) {
            throw new SigaRespuestaInvalidaException(
                    "El evento en el índice " + indice + " no incluye `" + campo + "` como número.");
        }
        return valor.asLong();
    }
}

