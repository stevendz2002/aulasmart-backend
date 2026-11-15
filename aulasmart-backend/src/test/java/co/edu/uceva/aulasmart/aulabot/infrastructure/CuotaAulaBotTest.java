package co.edu.uceva.aulasmart.aulabot.infrastructure;

import co.edu.uceva.aulasmart.aulabot.domain.model.AulaBotNoDisponibleException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CuotaAulaBotTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-14T18:00:00Z"), ZoneOffset.UTC);

    @Test void limitePublicoDiarioProtegeLaCuotaDeLasCuentas() {
        var cuota = new CuotaAulaBot(clock, 20, 6, 5);
        cuota.consumirVisitante("visitante:primero", true);
        cuota.consumirVisitante("visitante:segundo", true);

        assertThrows(AulaBotNoDisponibleException.class, () -> cuota.consumirVisitante("visitante:otro", true));
        assertDoesNotThrow(() -> cuota.consumir("cuenta@example.test"));
    }

    @Test void limitaTambienAccionesAnonimasAunqueCambieElIdentificador() {
        var cuota = new CuotaAulaBot(clock);
        for (int i = 0; i < 8; i++) cuota.consumirVisitante("visitante:" + i, false);

        assertThrows(AulaBotNoDisponibleException.class, () -> cuota.consumirVisitante("visitante:otro", false));
        assertDoesNotThrow(() -> cuota.consumir("cuenta@example.test"));
    }
}
