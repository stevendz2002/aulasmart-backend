package co.edu.uceva.aulasmart.users.infrastructure.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {

    private static final String RUTA_API = "/api/**";

    private final List<String> origenesCorsPermitidos;

    public SecurityConfiguration(
            @Value("${aulasmart.cors.allowed-origins:http://localhost:4200}") String origenesCorsPermitidos) {
        this.origenesCorsPermitidos = List.of(origenesCorsPermitidos.trim().split("\\s*,\\s*"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/solicitudes/*/revision").hasAnyRole(
                                "ADMINISTRADOR", "ADMINISTRATIVO"
                        )
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes").hasRole("DOCENTE")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/**").hasAnyRole(
                                "ADMINISTRADOR", "ADMINISTRATIVO", "DOCENTE"
                        )
                        .requestMatchers("/api/sincronizaciones/**").hasAnyRole(
                                "ADMINISTRADOR", "ADMINISTRATIVO"
                        )
                        .requestMatchers(HttpMethod.GET, "/api/aulas/**").hasAnyRole(
                                "ADMINISTRADOR", "ADMINISTRATIVO", "DOCENTE", "ESTUDIANTE"
                        )
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesCorsPermitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of(CorsConfiguration.ALL));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration(RUTA_API, configuracion);
        return fuente;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
