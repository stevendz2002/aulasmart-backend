package co.edu.uceva.aulasmart.users.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

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
    void noAplicaCorsPorFueraDeLaRutaApi() {
        assertNull(politicaPara("/actuator/health"));
    }
}
