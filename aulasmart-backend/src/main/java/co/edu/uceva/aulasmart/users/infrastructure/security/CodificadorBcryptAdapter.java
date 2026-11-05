package co.edu.uceva.aulasmart.users.infrastructure.security;

import co.edu.uceva.aulasmart.users.application.port.CodificadorClavesPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto de codificación de claves hacia el BCrypt de
 * Spring Security (mismo algoritmo que la autenticación HTTP Basic consulta).
 */
@Component
public class CodificadorBcryptAdapter implements CodificadorClavesPort {

    private final PasswordEncoder passwordEncoder;

    public CodificadorBcryptAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String codificar(String clavePlana) {
        return passwordEncoder.encode(clavePlana);
    }
}
