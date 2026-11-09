package co.edu.uceva.aulasmart.integrations.siga.infrastructure;

import co.edu.uceva.aulasmart.aulas.application.port.VerificacionEventosSigaPort;
import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.integrations.siga.infrastructure.http.SigaRestClient;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de verificación Just-In-Time contra SIGA.
 * Al solicitar una reserva, efectúa una consulta puntual y ligera al endpoint
 * público de SIGA para el aula específica. Si el aula está ocupada por una clase,
 * se detecta inmediatamente. Si SIGA falla, se interrumpe la operación porque
 * la base local no garantiza por sí sola la disponibilidad del intervalo.
 */
@Component
public class VerificacionEventosSigaAdapter implements VerificacionEventosSigaPort {

    private static final Logger REGISTRO = LoggerFactory.getLogger(VerificacionEventosSigaAdapter.class);
    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private final SigaRestClient cliente;

    public VerificacionEventosSigaAdapter(SigaRestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public boolean aulaTieneEventosEnSiga(Long codigoSigaAula, Intervalo intervalo) {
        if (codigoSigaAula == null || codigoSigaAula == 0) {
            throw new DisponibilidadNoVerificableException();
        }
        if (codigoSigaAula < 0) {
            // Aulas sintéticas de pruebas locales (código negativo) sin reflejo en SIGA
            return false;
        }

        try {
            LocalDate fechaInicio = LocalDate.ofInstant(intervalo.inicio(), ZONA_COLOMBIA);
            LocalDate fechaFin = LocalDate.ofInstant(intervalo.fin(), ZONA_COLOMBIA);

            List<EventoExterno> eventos = cliente.listarEventos(codigoSigaAula, fechaInicio, fechaFin);
            if (eventos == null) {
                throw new DisponibilidadNoVerificableException();
            }
            if (eventos.isEmpty()) {
                return false;
            }

            for (EventoExterno evento : eventos) {
                // Intervalo semiabierto [inicio, fin): se solapan si evento.inicio < intervalo.fin && intervalo.inicio < evento.fin
                if (evento.inicio().isBefore(intervalo.fin()) && intervalo.inicio().isBefore(evento.fin())) {
                    REGISTRO.warn("Validación JIT: Se detectó solapamiento en SIGA para el aula {} con evento '{}' entre {} y {}",
                            codigoSigaAula, evento.titulo(), evento.inicio(), evento.fin());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            REGISTRO.warn("No se pudo verificar la disponibilidad en SIGA para el aula {}: {}.",
                    codigoSigaAula, e.getMessage());
            throw new DisponibilidadNoVerificableException();
        }
    }
}

