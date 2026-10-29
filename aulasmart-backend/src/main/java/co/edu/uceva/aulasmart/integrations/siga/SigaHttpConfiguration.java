package co.edu.uceva.aulasmart.integrations.siga;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SigaPropiedades.class)
public class SigaHttpConfiguration {

    /**
     * Cliente HTTP con la base de los endpoints públicos SIGA. El detalle de la
     * URL de cada recurso vive en la implementación, no aquí.
     */
    @Bean
    RestClient restClientSiga(SigaPropiedades propiedades) {
        var clienteJdk = HttpClient.newBuilder()
                .connectTimeout(propiedades.timeout())
                .build();
        var fabrica = new JdkClientHttpRequestFactory(clienteJdk);
        fabrica.setReadTimeout(propiedades.timeout());
        return RestClient.builder()
                .baseUrl(propiedades.baseUrl())
                .requestFactory(fabrica)
                .build();
    }
}
