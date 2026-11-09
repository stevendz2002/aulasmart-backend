package co.edu.uceva.aulasmart.users.infrastructure.security;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigurationTest {

    private final SecurityConfiguration seguridad = new SecurityConfiguration(
            "http://localhost:4200, https://aulasmart.uceva.edu.co"
    );

    private CorsConfiguration politicaPara(String ruta) {
        var peticion = new MockHttpServletRequest();
        peticion.setServletPath(ruta);
        peticion.addHeader("Origin", "http://localhost:4200");
        peticion.addHeader("Access-Control-Request-Method", "GET");
        return seguridad.corsConfigurationSource().getCorsConfiguration(peticion);
    }

    @Test
    void defineLaPoliticaCorsSoloParaLaApiConOrigenesExplicitos() {
        var politica = politicaPara("/api/aulas");
        assertEquals(politica.getAllowedOrigins(), List.of("http://localhost:4200", "https://aulasmart.uceva.edu.co"));
        assertTrue(politica.getAllowCredentials());
        assertTrue(politica.getAllowedMethods().containsAll(List.of("GET", "POST", "OPTIONS")));
        assertTrue(politica.getAllowedHeaders().contains(CorsConfiguration.ALL));
    }

    @Test
    void permitePreflightPatchDeUsuariosSoloDesdeOrigenAutorizado() throws Exception {
        for (String origen : List.of("http://localhost:4200", "https://no-autorizado.example")) {
            var peticion = new MockHttpServletRequest("OPTIONS", "/api/usuarios/1");
            peticion.setServletPath("/api/usuarios/1");
            peticion.addHeader("Origin", origen);
            peticion.addHeader("Access-Control-Request-Method", "PATCH");
            peticion.addHeader("Access-Control-Request-Headers", "authorization,content-type");
            var respuesta = new MockHttpServletResponse();
            boolean permitido = new DefaultCorsProcessor().processRequest(
                    seguridad.corsConfigurationSource().getCorsConfiguration(peticion), peticion, respuesta);
            assertEquals(origen.equals("http://localhost:4200"), permitido);
            assertEquals(permitido ? 200 : 403, respuesta.getStatus());
        }
    }

    @Test
    void noAplicaCorsPorFueraDeLaRutaApi() {
        assertNull(politicaPara("/actuator/health"));
    }
}
