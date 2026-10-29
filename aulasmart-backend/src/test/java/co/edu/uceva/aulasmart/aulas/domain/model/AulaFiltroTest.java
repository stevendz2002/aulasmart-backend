package co.edu.uceva.aulasmart.aulas.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AulaFiltroTest {
    @Test
    void rechazaCapacidadMinimaNoPositiva() {
        assertThrows(IllegalArgumentException.class, () -> new AulaFiltro(null, null, null, 0));
    }
}
