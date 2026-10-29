package co.edu.uceva.aulasmart.integrations.siga.infrastructure.http;

import co.edu.uceva.aulasmart.aulas.domain.model.AulaExterna;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas HTTP a los endpoints públicos de SIGA verificados en
 * `docs/evidencias/01-fuentes-siga.md`. Cada respuesta se valida antes de
 * entregar datos a la aplicación; una respuesta cuestionada lanza
 * {@link SigaRespuestaInvalidaException}.
 */
@Component
public class SigaRestClient {

    private static final String RUTA_AULAS = "/publicomanejoespacios/listarAulasPublico";
    private static final String RUTA_EVENTOS = "/publicomanejoespacios/listarEventAulaPublico";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SigaRestClient(RestClient sigaRestClient, ObjectMapper objectMapper) {
        this.restClient = sigaRestClient;
        this.objectMapper = objectMapper;
    }

    public List<AulaExterna> listarAulas() {
        String cuerpo = restClient.get().uri(RUTA_AULAS).retrieve().body(String.class);
        requerirCuerpo(cuerpo, RUTA_AULAS);
        return SigaAulasRespuesta.desde(objectMapper, cuerpo).aulas();
    }

    public SigaEventosRespuesta listarEventos(LocalDate desde, LocalDate hasta) {
        String cuerpo = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(RUTA_EVENTOS)
                        .queryParam("startDate", desde)
                        .queryParam("endDate", hasta)
                        .build())
                .retrieve()
                .body(String.class);
        requerirCuerpo(cuerpo, RUTA_EVENTOS);
        return SigaEventosRespuesta.desde(objectMapper, cuerpo);
    }

    private void requerirCuerpo(String cuerpo, String ruta) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new SigaRespuestaInvalidaException("El endpoint " + ruta + " devolvió un cuerpo vacío.");
        }
    }
}
