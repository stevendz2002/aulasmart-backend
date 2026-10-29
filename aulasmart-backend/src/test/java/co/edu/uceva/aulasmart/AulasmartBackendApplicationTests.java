package co.edu.uceva.aulasmart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AulasmartBackendApplicationTests {
    @Test
    void laClasePrincipalTieneConfiguracionSpringBoot() {
        assertNotNull(AulasmartBackendApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
