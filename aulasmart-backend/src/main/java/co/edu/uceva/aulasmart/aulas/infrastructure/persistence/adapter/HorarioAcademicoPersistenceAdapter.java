package co.edu.uceva.aulasmart.aulas.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.aulas.application.port.HorarioAcademicoPort;
import co.edu.uceva.aulasmart.aulas.domain.model.EventoExterno;
import co.edu.uceva.aulasmart.aulas.domain.model.Intervalo;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.entity.HorarioAcademicoEntity;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository.AulaJpaRepository;
import co.edu.uceva.aulasmart.aulas.infrastructure.persistence.repository.HorarioAcademicoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Persistencia de la ocupación académica sincronizada. La escritura es
 * atómica: en una misma transacción descarta lo conocido desde el inicio de
 * la ventana e inserta solo las ocurrencias cuyo aula sigue presente en el
 * catálogo (las que ya no existe no pueden ocuparse).
 */
@Component
public class HorarioAcademicoPersistenceAdapter implements HorarioAcademicoPort {

    private final HorarioAcademicoJpaRepository horarios;
    private final AulaJpaRepository aulas;

    public HorarioAcademicoPersistenceAdapter(
            HorarioAcademicoJpaRepository horarios,
            AulaJpaRepository aulas
    ) {
        this.horarios = horarios;
        this.aulas = aulas;
    }

    @Override
    @Transactional
    public int aplicarEventosExternos(
            List<EventoExterno> eventos,
            Instant inicioVentana,
            Instant finVentana
    ) {
        var entidades = new ArrayList<HorarioAcademicoEntity>(eventos.size());
        for (EventoExterno evento : eventos) {
            aulas.findByCodigoSiga(evento.codigoAulaSiga()).ifPresent(aula ->
                    entidades.add(HorarioAcademicoEntity.crear(
                            aula.getId(),
                            evento.idExterno(),
                            evento.inicio(),
                            evento.fin()
                    ))
            );
        }

        horarios.eliminarDesde(inicioVentana);
        horarios.saveAll(entidades);
        return entidades.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> buscarAulasConEventos(Intervalo intervalo) {
        return horarios.buscarAulasConEventos(intervalo.inicio(), intervalo.fin());
    }
}
