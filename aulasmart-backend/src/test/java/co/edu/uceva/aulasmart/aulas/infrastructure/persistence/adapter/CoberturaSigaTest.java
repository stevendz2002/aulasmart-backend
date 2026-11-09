package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.SincronizacionSigaEntity;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository.SincronizacionSigaJpaRepository;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoberturaSigaTest {
    private final Instant inicio = Instant.parse("2026-11-08T05:00:00Z");
    private final Instant fin = inicio.plusSeconds(7 * 86400);

    @Test
    void exigeCoberturaCompletaYRespetaLimites() {
        var corrida = SincronizacionSigaEntity.crear("EVENTOS", inicio);
        corrida.registrarCobertura(inicio, fin);
        corrida.finalizar("EXITOSA", inicio.plusSeconds(30), 0, null);
        assertTrue(corrida.cubre(inicio, fin, inicio));
        assertFalse(corrida.cubre(inicio.minusSeconds(1), fin, inicio));
        assertFalse(corrida.cubre(inicio, fin.plusSeconds(1), inicio));
        assertFalse(corrida.cubre(inicio, fin, inicio.plusSeconds(31)));
    }

    @Test
    void corridaHistoricaSinCoberturaNoEsSuficiente() {
        var corrida = SincronizacionSigaEntity.crear("EVENTOS", inicio);
        corrida.finalizar("EXITOSA", inicio, 10, null);
        assertFalse(corrida.cubre(inicio, fin, inicio));
    }

    @Test
    void noReutilizaUnaCorridaAnteriorMientrasLaUltimaFallaOEstaEnCurso() {
        var repository = mock(SincronizacionSigaJpaRepository.class);
        var adapter = new SincronizacionSigaPersistenceAdapter(repository);
        var catalogo = SincronizacionSigaEntity.crear("AULAS", inicio.minusSeconds(1));
        catalogo.finalizar("EXITOSA", inicio, 1, null);
        when(repository.findFirstByRecursoOrderByIdDesc("AULAS")).thenReturn(Optional.of(catalogo));
        var corrida = SincronizacionSigaEntity.crear("EVENTOS", inicio);
        corrida.registrarCobertura(inicio, fin);
        when(repository.findFirstByRecursoOrderByIdDesc("EVENTOS")).thenReturn(Optional.of(corrida));
        assertFalse(adapter.eventosCubren(inicio, new Intervalo(inicio, fin)));
        corrida.finalizar("FALLIDA", inicio, 0, "fallo");
        assertFalse(adapter.eventosCubren(inicio, new Intervalo(inicio, fin)));
        corrida.finalizar("EXITOSA", inicio, 0, null);
        assertTrue(adapter.eventosCubren(inicio, new Intervalo(inicio, fin)));
        catalogo.finalizar("EXITOSA", inicio.plusSeconds(1), 2, null);
        assertFalse(adapter.eventosCubren(inicio, new Intervalo(inicio, fin)));
    }
}
