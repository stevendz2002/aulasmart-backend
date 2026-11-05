package co.edu.uceva.aulasmart.integrations.siga.presentation;

import co.edu.uceva.aulasmart.integrations.siga.application.model.ResultadoSincronizacion;
import co.edu.uceva.aulasmart.integrations.siga.application.service.SincronizacionSigaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * Disparador manual de sincronizaciones, restringido por rol. La ejecución
 * programada usa el mismo servicio, de modo que ambos caminos comparten las
 * mismas reglas y registros de control. Recursos sincronizables: AULAS
 * (catálogo) y EVENTOS (ocupación por clases del calendario público).
 */
@RestController
@RequestMapping("/api/sincronizaciones")
public class SincronizacionController {

    private final SincronizacionSigaService servicio;

    public SincronizacionController(SincronizacionSigaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public List<SincronizacionResponse> registrar(
            @RequestParam(defaultValue = "AULAS") String recurso
    ) {
        List<String> recursos = switch (recurso.toUpperCase(Locale.ROOT)) {
            case "AULAS" -> List.of("AULAS");
            case "EVENTOS" -> List.of("EVENTOS");
            default -> throw new IllegalArgumentException(
                    "Recurso de sincronización desconocido: `" + recurso
                            + "`. Valores de sincronización válidos: AULAS, EVENTOS.");
        };
        return servicio.sincronizar(recursos).stream()
                .map(SincronizacionResponse::desde)
                .toList();
    }
}
