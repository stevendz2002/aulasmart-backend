package co.edu.uceva.aulasmart.shared.presentation;

import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RestExceptionHandlerTest {
    @Test
    void devuelve503CuandoLaDisponibilidadNoEsVerificable() throws Exception {
        var mvc = standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new RestExceptionHandler()).build();

        mvc.perform(get("/prueba-disponibilidad"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("status").value(503))
                .andExpect(jsonPath("message").value(new DisponibilidadNoVerificableException().getMessage()));
    }

    @RestController
    static class ControladorDePrueba {
        @GetMapping("/prueba-disponibilidad")
        public void consultar() {
            throw new DisponibilidadNoVerificableException();
        }
    }
}
