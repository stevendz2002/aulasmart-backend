package co.edu.uceva.aulasmart.users.infrastructure.security;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtConfiguration {
    @Bean
    SecretKey jwtKey(@Value("${aulasmart.auth.secret:}") String encoded, Environment environment) {
        byte[] bytes;
        if (encoded.isBlank()) {
            if (environment.matchesProfiles("prod") || !environment.matchesProfiles("local", "test"))
                throw new IllegalStateException("Configure AULASMART_JWT_SECRET para este entorno.");
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(encoded);
            if (bytes.length < 32) throw new IllegalStateException("La clave JWT requiere al menos 32 bytes aleatorios en Base64.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, @Value("${aulasmart.auth.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getExpiresAt() != null && jwt.getIssuedAt() != null
                && jwt.getAudience() != null && jwt.getAudience().contains("aulasmart-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audience));
        return decoder;
    }
}
