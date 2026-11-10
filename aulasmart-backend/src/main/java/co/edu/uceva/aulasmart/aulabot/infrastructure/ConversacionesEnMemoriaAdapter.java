package co.edu.uceva.aulasmart.aulabot.infrastructure;

import co.edu.uceva.aulasmart.aulabot.application.port.RegistroConversacionesPort;
import co.edu.uceva.aulasmart.aulabot.domain.model.EstadoConversacionAulaBot;
import co.edu.uceva.aulasmart.aulabot.domain.model.InterpretacionAulaBot;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Memoria y deduplicación de mensajes, sin Redis ni base de datos: por usuario
 * guarda el contexto estructurado vigente y unas cuantas interpretaciones
 * idénticas recientes. La única reutilización válida es la del mismo mensaje
 * bajo el mismo estado; si el estado cambió, se interpreta de nuevo.
 */
@Component
public class ConversacionesEnMemoriaAdapter implements RegistroConversacionesPort {

    static final int MAX_USUARIOS = 500;
    private static final int MAX_REPETICIONES = 8;

    private final Clock clock;
    private final Duration vigencia;
    private final Duration vigenciaRepeticion;
    private final Map<String, Conversacion> conversaciones = new HashMap<>();

    private record Conversacion(EstadoConversacionAulaBot estado, Map<String, Repetida> recientes) {}
    private record Repetida(InterpretacionAulaBot interpretacion, Instant vence, String firmaEstado) {}

    public ConversacionesEnMemoriaAdapter(Clock clock,
            @Value("${aulasmart.aulabot.contexto-ttl:PT30M}") Duration vigencia,
            @Value("${aulasmart.aulabot.deduplicacion-ttl:PT90S}") Duration vigenciaRepeticion) {
        if (vigencia == null || vigencia.toMinutes() < 1 || vigencia.toHours() > 12) {
            throw new IllegalArgumentException("La vigencia del contexto debe quedar entre 1 minuto y 12 horas.");
        }
        if (vigenciaRepeticion == null || vigenciaRepeticion.isNegative() || vigenciaRepeticion.isZero()
                || vigenciaRepeticion.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException("La vigencia de deduplicación debe quedar entre 1 segundo y 10 minutos.");
        }
        this.clock = clock;
        this.vigencia = vigencia;
        this.vigenciaRepeticion = vigenciaRepeticion;
    }

    @Override
    public synchronized EstadoConversacionAulaBot estado(String usuario, boolean reiniciar) {
        var clave = claveUsuario(usuario);
        if (reiniciar) {
            conversaciones.remove(clave);
            return EstadoConversacionAulaBot.inicial(clock.instant());
        }
        Instant ahora = clock.instant();
        var conversacion = conversaciones.get(clave);
        if (conversacion == null
                || conversacion.estado().actualizadoEn().isBefore(ahora.minus(vigencia))) {
            return EstadoConversacionAulaBot.inicial(ahora);
        }
        return conversacion.estado();
    }

    @Override
    public synchronized void guardar(String usuario, EstadoConversacionAulaBot estado) {
        Objects.requireNonNull(estado, "El estado de la conversación es obligatorio.");
        var clave = claveUsuario(usuario);
        var existente = conversaciones.get(clave);
        var recientes = existente == null ? new HashMap<String, Repetida>() : existente.recientes();
        if (estado.vacia() && recientes.isEmpty()) {
            conversaciones.remove(clave);
            return;
        }
        conversaciones.put(clave, new Conversacion(estado, recientes));
        limpiarVencidas(clock.instant());
        expulsarElMasViejoSiExcede();
    }

    @Override
    public synchronized Optional<InterpretacionAulaBot> repeticionReciente(String usuario, String mensaje,
            EstadoConversacionAulaBot estado) {
        var conversacion = conversaciones.get(claveUsuario(usuario));
        if (conversacion == null || estado == null) {
            return Optional.empty();
        }
        Instant ahora = clock.instant();
        var clave = claveMensaje(mensaje);
        var repetida = conversacion.recientes().get(clave);
        if (repetida == null || !repetida.vence().isAfter(ahora)) {
            conversacion.recientes().remove(clave);
            return Optional.empty();
        }
        if (!repetida.firmaEstado().equals(firmaEstado(estado))) {
            return Optional.empty();
        }
        return Optional.of(repetida.interpretacion());
    }

    @Override
    public synchronized void registrar(String usuario, String mensaje, EstadoConversacionAulaBot estado,
            InterpretacionAulaBot interpretacion) {
        if (interpretacion == null) {
            return;
        }
        var clave = claveUsuario(usuario);
        var conversacion = conversaciones.computeIfAbsent(clave, k -> new Conversacion(
                estado == null ? EstadoConversacionAulaBot.inicial(clock.instant()) : estado, new HashMap<>()));
        conversacion.recientes().put(claveMensaje(mensaje),
                new Repetida(interpretacion, clock.instant().plus(vigenciaRepeticion), firmaEstado(estado)));
        var recientes = conversacion.recientes();
        while (recientes.size() > MAX_REPETICIONES) {
            recientes.remove(recientes.keySet().iterator().next());
        }
    }

    private static String firmaEstado(EstadoConversacionAulaBot estado) {
        if (estado == null) {
            return "-";
        }
        return (estado.consultaVigente() == null ? "-" : estado.consultaVigente().toString())
                + "|" + (estado.aclaracionPendiente() == null ? "-" : estado.aclaracionPendiente())
                + "|" + (estado.franjaPendiente() == null ? "-" : estado.franjaPendiente().toString());
    }

    private static String claveUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("La identidad del usuario es obligatoria.");
        }
        return usuario.strip().toLowerCase(Locale.ROOT);
    }

    private static String claveMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje es obligatorio para la deduplicación.");
        }
        return Normalizer.normalize(mensaje, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private void limpiarVencidas(Instant ahora) {
        Instant limite = ahora.minus(vigencia);
        conversaciones.entrySet().removeIf(e ->
                e.getValue().estado().actualizadoEn().isBefore(limite));
    }

    private void expulsarElMasViejoSiExcede() {
        while (conversaciones.size() > MAX_USUARIOS) {
            String masVieja = null;
            Instant mejor = null;
            for (var entrada : conversaciones.entrySet()) {
                var marca = entrada.getValue().estado().actualizadoEn();
                if (mejor == null || marca.isBefore(mejor)) {
                    mejor = marca;
                    masVieja = entrada.getKey();
                }
            }
            if (masVieja == null) {
                return;
            }
            conversaciones.remove(masVieja);
        }
    }
}
