package co.edu.uceva.aulasmart.aulas.domain.model;

import co.edu.uceva.aulasmart.aulas.domain.exception.IntervaloInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntervaloTest {
    private static final Instant INICIO = Instant.parse("2026-11-10T13:00:00Z");
    private static final Instant FIN = Instant.parse("2026-11-10T15:00:00Z");

    @Test
    void creaUnIntervaloValido() {
        var intervalo = new Intervalo(INICIO, FIN);

        assertEquals(INICIO, intervalo.inicio());
        assertEquals(FIN, intervalo.fin());
    }

    @Test
    void rechazaUnFinIgualAlInicio() {
        assertThrows(IntervaloInvalidoException.class, () -> new Intervalo(INICIO, INICIO));
    }

    @Test
    void rechazaUnFinAnteriorAlInicio() {
        assertThrows(IntervaloInvalidoException.class, () -> new Intervalo(FIN, INICIO));
    }
}
