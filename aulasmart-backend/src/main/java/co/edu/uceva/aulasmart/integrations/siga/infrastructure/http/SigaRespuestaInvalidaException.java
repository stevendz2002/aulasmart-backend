package co.edu.uceva.aulasmart.integrations.siga.infrastructure.http;

/**
 * Indica que la respuesta de SIGA no cumplió el formato verificado en
 * `docs/evidencias/01-fuentes-siga.md`. Nadie debe interpretar una respuesta
 * fuera de esa evidencia.
 */
public class SigaRespuestaInvalidaException extends RuntimeException {

    public SigaRespuestaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public SigaRespuestaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
