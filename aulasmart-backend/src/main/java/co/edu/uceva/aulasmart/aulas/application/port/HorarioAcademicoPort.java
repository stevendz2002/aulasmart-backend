package co.edu.uceva.aulasmart.aulas.application.port;

import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.aulas.domain.model.OcupacionAula;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Ocupación académica sincronizada desde SIGA (tabla `horario_academico`).
 * La escritura reemplaza por ventana: al aplicar una corrida, las filas con
 * fuente SIGA a partir del inicio de la ventana se descartan y se inserta el
 * contenido fresco. La lectura responde qué aulas están ocupadas en un
 * intervalo y se une con las reservas aprobadas al calcular disponibilidad.
 */
public interface HorarioAcademicoPort {

    /**
     * Aplica atómicamente la lista de eventos externos y devuelve la cantidad
     * de ocurrencias persistidas.
     */
    int aplicarEventosExternos(
            List<EventoExterno> eventos,
            Instant inicioVentana,
            Instant finVentana
    );

    Set<Long> buscarAulasConEventos(Intervalo intervalo);

    /** Ocupación de un aula en el intervalo (para la ficha de detalles). */
    List<OcupacionAula> buscarOcupacionDeAula(Long idAula, Intervalo intervalo);
}
