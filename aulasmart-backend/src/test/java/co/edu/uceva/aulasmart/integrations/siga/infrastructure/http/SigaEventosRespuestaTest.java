package co.edu.uceva.aulasmart.integrations.siga.infrastructure.http;

import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida la interpretación de la respuesta real del calendario público de
 * SIGA, contra muestras observadas del módulo `publicomanejoespacios`.
 */
class SigaEventosRespuestaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void interpretaLaMuestraRealDeUnAulaConClases() {
        String cuerpo = """
                {
                  "success": true,
                  "evts": [
                    {
                      "id_unico": "2026071320->22034-220->20192->1",
                      "evt_id": 161510,
                      "cal_id": 1,
                      "evt_title": "Asignatura: 22034-220 - COSTOS Y PRESUPUESTOS",
                      "start_dt": "2026-11-03 07:20:00",
                      "end_dt": "2026-11-03 09:00:00",
                      "date_start": "2026-11-03",
                      "date_end": "2026-11-03",
                      "code_location": 195,
                      "cancelar": false,
                      "type_booking": 1,
                      "calendario_es_de_programacion_academica": true
                    },
                    {
                      "id_unico": "2026071320->26030-260->20201->1",
                      "evt_id": 161904,
                      "start_dt": "2026-11-02 08:10:00",
                      "end_dt": "2026-11-02 09:40:00",
                      "code_location": 195,
                      "cancelar": false
                    }
                  ],
                  "total": 2
                }
                """;

        SigaEventosRespuesta respuesta = SigaEventosRespuesta.desde(MAPPER, cuerpo);

        assertEquals(2, respuesta.total());
        List<EventoExterno> eventos = respuesta.eventos();
        assertEquals(2, eventos.size());

        var primera = eventos.get(0);
        assertEquals(195L, primera.codigoAulaSiga());
        assertEquals("161510|2026-11-03 07:20:00", primera.idExterno());
        assertEquals(Instant.parse("2026-11-03T12:20:00Z"), primera.inicio());
        assertEquals(Instant.parse("2026-11-03T14:00:00Z"), primera.fin());

        var segunda = eventos.get(1);
        assertEquals("161904|2026-11-02 08:10:00", segunda.idExterno());
        assertEquals(Instant.parse("2026-11-02T13:10:00Z"), segunda.inicio());
    }

    @Test
    void descartaLasClasesCanceladasPeroAceptaLaMuestra() {
        String cuerpo = """
                {
                  "success": true,
                  "evts": [
                    {"evt_id": 100, "start_dt": "2026-11-05 07:00:00", "end_dt": "2026-11-05 08:00:00",
                     "code_location": 195, "cancelar": true},
                    {"evt_id": 101, "start_dt": "2026-11-05 07:00:00", "end_dt": "2026-11-05 08:00:00",
                     "code_location": 196, "cancelar": false}
                  ],
                  "total": 2
                }
                """;

        SigaEventosRespuesta respuesta = SigaEventosRespuesta.desde(MAPPER, cuerpo);

        assertEquals(2, respuesta.total());
        assertEquals(1, respuesta.eventos().size());
        assertEquals(196L, respuesta.eventos().get(0).codigoAulaSiga());
    }

    @Test
    void rechazaLaRespuestaDeParametrosInvalidosAunqueIndiqueSuccess() {
        String cuerpo = """
                {"success":true,"evts":[],"message":"Parametros invalidos","total":0}
                """;

        SigaRespuestaInvalidaException excepcion = assertThrows(
                SigaRespuestaInvalidaException.class,
                () -> SigaEventosRespuesta.desde(MAPPER, cuerpo)
        );
        assertTrue(excepcion.getMessage().contains("Parametros invalidos"));
    }

    @Test
    void aceptaRespuestaVaciaDeUnAulaSinEventos() {
        String cuerpo = """
                {"success":true,"evts":[],"total":0}
                """;

        SigaEventosRespuesta respuesta = SigaEventosRespuesta.desde(MAPPER, cuerpo);

        assertEquals(0, respuesta.total());
        assertTrue(respuesta.eventos().isEmpty());
    }

    @Test
    void rechazaTotalesQueNoCoincidenConLaCantidadRecibida() {
        String cuerpo = """
                {
                  "success": true,
                  "evts": [
                    {"evt_id": 100, "start_dt": "2026-11-05 07:00:00", "end_dt": "2026-11-05 08:00:00",
                     "code_location": 195, "cancelar": false}
                  ],
                  "total": 7
                }
                """;

        assertThrows(SigaRespuestaInvalidaException.class,
                () -> SigaEventosRespuesta.desde(MAPPER, cuerpo));
    }

    @Test
    void rechazaFechasFueraDelFormatoObservado() {
        String cuerpo = """
                {
                  "success": true,
                  "evts": [
                    {"evt_id": 100, "start_dt": "05/11/2026 07:00", "end_dt": "05/11/2026 08:00",
                     "code_location": 195, "cancelar": false}
                  ],
                  "total": 1
                }
                """;

        assertThrows(SigaRespuestaInvalidaException.class,
                () -> SigaEventosRespuesta.desde(MAPPER, cuerpo));
    }

    @Test
    void rechazaEventosQueTerminanIgualOIrregularmente() {
        String cuerpo = """
                {
                  "success": true,
                  "evts": [
                    {"evt_id": 100, "start_dt": "2026-11-05 07:00:00", "end_dt": "2026-11-05 07:00:00",
                     "code_location": 195, "cancelar": false}
                  ],
                  "total": 1
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> SigaEventosRespuesta.desde(MAPPER, cuerpo));
    }
}
