package co.edu.uceva.aulasmart.users.infrastructure.security;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class JwtConfigurationTest {
    private final JwtConfiguration config=new JwtConfiguration();
    @Test void produccionRechazaClaveAusenteInclusoSiSeActivaLocal() {
        var env=new MockEnvironment(); env.setActiveProfiles("prod","local");
        assertThrows(IllegalStateException.class,() -> config.jwtKey("",env));
    }
    @Test void claveCortaOBase64InvalidaSeRechazan() {
        var env=new MockEnvironment(); env.setActiveProfiles("local");
        assertThrows(IllegalStateException.class,() -> config.jwtKey(Base64.getEncoder().encodeToString(new byte[16]),env));
        assertThrows(IllegalArgumentException.class,() -> config.jwtKey("no-base64!",env));
    }
    @Test void soloLocalPermiteClaveEfimeraYEsDistintaPorArranque() {
        var env=new MockEnvironment(); env.setActiveProfiles("local");
        assertFalse(java.util.Arrays.equals(config.jwtKey("",env).getEncoded(),config.jwtKey("",env).getEncoded()));
        assertThrows(IllegalStateException.class,() -> config.jwtKey("",new MockEnvironment()));
    }
}
