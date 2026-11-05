package co.edu.uceva.aulasmart.integrations.siga.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SigaAulasRespuestaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void interpretaUnRegistroConLosCamposVerificados() {
        String cuerpo = """
                {
                  "success": true,
                  "total": 1,
                  "data": [
                    {
                      "codigo_aula": 406,
                      "nombre_aula": "AULAS SEDE CALI (406)",
                      "codigo_edificio": 48,
                      "nombre_edificio": "MULTICAMPUS LADERA",
                      "sillas": 200,
                      "codigo_dependencia": 14,
                      "nombre_dependencia": "INFORMATICA",
                      "codigo_tipo_aula": 1,
                      "nombre_tipo_aula": "SALON DE CLASE",
                      "view": "<div>redundante</div>",
                      "iconcls": "icon_salon_clase"
                    }
                  ]
                }
                """;

        var respuesta = SigaAulasRespuesta.desde(objectMapper, cuerpo);

        assertEquals(1, respuesta.total());
        var aula = respuesta.aulas().get(0);
        assertEquals(406L, aula.codigoAula());
        assertEquals("AULAS SEDE CALI (406)", aula.nombreAula());
        assertEquals(48L, aula.codigoEdificio());
        assertEquals(200, aula.sillas());
        assertEquals(14L, aula.codigoDependencia());
        assertEquals("INFORMATICA", aula.nombreDependencia());
        assertEquals(1L, aula.codigoTipoAula());
        assertEquals("SALON DE CLASE", aula.nombreTipoAula());
    }

    @Test
    void rechazaRespuestaSinExito() {
        String cuerpo = "{\"success\":false,\"data\":[],\"total\":0}";

        var excepcion = assertThrows(SigaRespuestaInvalidaException.class,
                () -> SigaAulasRespuesta.desde(objectMapper, cuerpo));

        assertEquals("El endpoint de aulas devolvió success=false.", excepcion.getMessage());
    }

    @Test
    void rechazaTotalInconsistenteConLaCantidadRecibida() {
        String cuerpo = """
                {
                  "success": true,
                  "total": 104,
                  "data": [
                    {"codigo_aula": 1, "nombre_aula": "PRUEBA Aula", "codigo_edificio": 1,
                     "nombre_edificio": "PRUEBA Edificio", "sillas": 15,
                     "codigo_dependencia": 1, "nombre_dependencia": "PRUEBA Dependencia",
                     "codigo_tipo_aula": 1, "nombre_tipo_aula": "PRUEBA Tipo"}
                  ]
                }
                """;

        var excepcion = assertThrows(SigaRespuestaInvalidaException.class,
                () -> SigaAulasRespuesta.desde(objectMapper, cuerpo));

        assertEquals("El total declarado (104) no coincide con la cantidad recibida (1). "
                + "El endpoint no respondió a la paginación solicitada; se rechaza de forma conservadora.",
                excepcion.getMessage());
    }

    @Test
    void rechazaRegistroSinCampoEstructurado() {
        String cuerpo = """
                {
                  "success": true,
                  "total": 1,
                  "data": [
                    {"codigo_aula": 1, "nombre_aula": "PRUEBA Aula", "sillas": 15}
                  ]
                }
                """;

        var excepcion = assertThrows(SigaRespuestaInvalidaException.class,
                () -> SigaAulasRespuesta.desde(objectMapper, cuerpo));

        assertEquals("El aula en el índice 0 no incluye `codigo_edificio` como número.", excepcion.getMessage());
    }

    @Test
    void rechazaCuerpoQueNoEsJson() {
        assertThrows(SigaRespuestaInvalidaException.class,
                () -> SigaAulasRespuesta.desde(objectMapper, "<html>error</html>"));
    }
}
