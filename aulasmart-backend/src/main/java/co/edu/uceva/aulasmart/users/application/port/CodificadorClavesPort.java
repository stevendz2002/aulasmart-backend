package co.edu.uceva.aulasmart.users.application.port;

/**
 * Codificación de claves para el dominio de usuarios. La implementación
 * concreta (BCrypt de Spring Security) vive en infraestructura.
 */
public interface CodificadorClavesPort {

    String codificar(String clavePlana);
}
