package co.edu.uceva.aulasmart.integrations.siga;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración del acceso a SIGA. Los valores por defecto se declaran en
 * `application.properties` y se registran mediante `@EnableConfigurationProperties`.
 */
@ConfigurationProperties(prefix = "aulasmart.siga")
public record SigaPropiedades(String baseUrl, Duration timeout, Eventos eventos, Programado programado) {

    /**
     * Ventana de eventos que la sincronización debe intentar cubrir en cada
     * corrida, medida desde la fecha institucional actual.
     */
    public record Eventos(int diasHaciaAtras, int diasHaciaAdelante) {
    }

    public record Programado(String cron, String zona) {
    }
}
