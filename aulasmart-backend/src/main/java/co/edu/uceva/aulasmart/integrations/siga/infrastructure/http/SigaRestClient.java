package co.edu.uceva.aulasmart.integrations.siga.infrastructure.http;

import co.edu.uceva.aulasmart.aulas.domain.model.AulaExterna;
import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas HTTP a los endpoints públicos de SIGA verificados en
 * `docs/evidencias/01-fuentes-siga.md` y en el módulo `publicomanejoespacios`:
 * el catálogo de aulas (`listarAulasPublico`) y el calendario de ocupación por
 * aula (`listarEventAulaPublico`). Cada respuesta se valida antes de entregar
 * datos a la aplicación; una respuesta cuestionada lanza
 * {@link SigaRespuestaInvalidaException}.
 */
@Component
public class SigaRestClient {

    private static final String RUTA_AULAS = "/publicomanejoespacios/listarAulasPublico";
    private static final String RUTA_EVENTOS = "/publicomanejoespacios/listarEventAulaPublico";
    /** Valor del calendario público observado en el módulo de espacios. */
    private static final String CALENDARIO_PUBLICO = "calendario_publico";

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

    /**
     * Ocupación de un aula en el rango de fechas [inicio, fin] inclusive,
     * tal como la pide el calendario público: una petición por aula con
     * `calendario=calendario_publico` y `startDate`/`endDate` en formato ISO.
     */
    public List<EventoExterno> listarEventos(Long codigoAula, LocalDate inicio, LocalDate fin) {
        String ruta = rutaEventos(codigoAula, inicio, fin);
        String cuerpo = restClient.get().uri(ruta).retrieve().body(String.class);
        requerirCuerpo(cuerpo, RUTA_EVENTOS);
        return SigaEventosRespuesta.desde(objectMapper, cuerpo).eventos();
    }

    /** Construye la ruta con parámetros correctamente codificados. */
    private static String rutaEventos(Long codigoAula, LocalDate inicio, LocalDate fin) {
        return UriComponentsBuilder.fromPath(RUTA_EVENTOS)
                .queryParam("calendario", CALENDARIO_PUBLICO)
                .queryParam("configuracion", "{}")
                .queryParam("aula", codigoAula)
                .queryParam("startDate", inicio)
                .queryParam("endDate", fin)
                .build()
                .toUriString();
    }

    private void requerirCuerpo(String cuerpo, String ruta) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new SigaRespuestaInvalidaException("El endpoint " + ruta + " devolvió un cuerpo vacío.");
        }
    }
}
