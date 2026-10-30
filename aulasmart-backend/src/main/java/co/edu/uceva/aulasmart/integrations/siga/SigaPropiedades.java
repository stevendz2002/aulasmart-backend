package co.edu.uceva.aulasmart.integrations.siga;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración del acceso a SIGA. Los valores por defecto se declaran en
 * `application.properties` y se registran mediante `@EnableConfigurationProperties`.
 */
@ConfigurationProperties(prefix = "aulasmart.siga")
public record SigaPropiedades(String baseUrl, Duration timeout, Programado programado) {

    public record Programado(String cron, String zona) {
    }
}
