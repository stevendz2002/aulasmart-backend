package co.edu.uceva.aulasmart.integrations.siga.application.service;

import co.edu.uceva.aulasmart.integrations.siga.application.model.ResultadoSincronizacion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Coordinador de corridas de sincronización. Un método `synchronized` evita
 * que una corrida manual y la programada se solapen en la misma instancia del
 * monolito.
 */
@Service
public class SincronizacionSigaService {

    private static final Set<String> RECURSOS_VALIDOS = Set.of(
            SincronizadorDeAulasService.RECURSO,
            SincronizadorDeEventosService.RECURSO
    );

    private final SincronizadorDeAulasService sincronizadorDeAulas;
    private final SincronizadorDeEventosService sincronizadorDeEventos;

    public SincronizacionSigaService(
            SincronizadorDeAulasService sincronizadorDeAulas,
            SincronizadorDeEventosService sincronizadorDeEventos
    ) {
        this.sincronizadorDeAulas = sincronizadorDeAulas;
        this.sincronizadorDeEventos = sincronizadorDeEventos;
    }

    public List<ResultadoSincronizacion> sincronizar(List<String> recursos) {
        List<ResultadoSincronizacion> resultados = new ArrayList<>(recursos.size());
        for (String recurso : recursos) {
            resultados.add(ejecutar(recurso));
        }
        return List.copyOf(resultados);
    }

    public synchronized ResultadoSincronizacion ejecutar(String recurso) {
        String recursoNormalizado = recurso.toUpperCase(Locale.ROOT);
        if (!RECURSOS_VALIDOS.contains(recursoNormalizado)) {
            throw new IllegalArgumentException("Recurso de sincronización desconocido: `" + recurso
                    + "`. Valores válidos: AULAS, EVENTOS.");
        }
        return switch (recursoNormalizado) {
            case SincronizadorDeAulasService.RECURSO -> sincronizadorDeAulas.sincronizar();
            case SincronizadorDeEventosService.RECURSO -> sincronizadorDeEventos.sincronizar();
            default -> throw new IllegalArgumentException("Recurso de sincronización desconocido: `" + recurso + "`.");
        };
    }
}
