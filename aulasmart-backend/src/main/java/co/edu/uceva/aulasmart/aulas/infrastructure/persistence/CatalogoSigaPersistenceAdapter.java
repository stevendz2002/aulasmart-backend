package co.edu.uceva.aulasmart.aulas.infrastructure.persistence;

import co.edu.uceva.aulasmart.aulas.application.port.CatalogoSigaPort;
import co.edu.uceva.aulasmart.aulas.domain.model.AulaExterna;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aplica el catálogo SIGA sobre las relaciones locales. Los catálogos se
 * resuelven por `codigo_siga` (se reutilizan entre aulas y se crean si no
 * existen) y cada aula se crea si es nueva o se actualiza si ya existe.
 * `aula.activa` no se modifica: es un atributo local de AulaSmart.
 */
@Component
public class CatalogoSigaPersistenceAdapter implements CatalogoSigaPort {

    private final EdificioJpaRepository edificios;
    private final TipoAulaJpaRepository tiposAula;
    private final DependenciaJpaRepository dependencias;
    private final AulaJpaRepository aulas;

    public CatalogoSigaPersistenceAdapter(
            EdificioJpaRepository edificios,
            TipoAulaJpaRepository tiposAula,
            DependenciaJpaRepository dependencias,
            AulaJpaRepository aulas
    ) {
        this.edificios = edificios;
        this.tiposAula = tiposAula;
        this.dependencias = dependencias;
        this.aulas = aulas;
    }

    @Override
    @Transactional
    public int aplicarAulasExternas(List<AulaExterna> aulasExternas) {
        // El reuso dentro de la misma corrida evita consultas repetidas por aula
        // y garantiza una única fila de cada catálogo por código SIGA.
        Map<Long, EdificioEntity> edificiosResueltos = new HashMap<>();
        Map<Long, TipoAulaEntity> tiposResueltos = new HashMap<>();
        Map<Long, DependenciaEntity> dependenciasResueltas = new HashMap<>();

        for (AulaExterna externa : aulasExternas) {
            var edificio = resolverEdificio(externa.codigoEdificio(), externa.nombreEdificio(), edificiosResueltos);
            var tipoAula = resolverTipoAula(externa.codigoTipoAula(), externa.nombreTipoAula(), tiposResueltos);
            var dependencia = resolverDependencia(externa.codigoDependencia(), externa.nombreDependencia(), dependenciasResueltas);

            var aula = aulas.findByCodigoSiga(externa.codigoAula()).orElse(null);
            if (aula == null) {
                aulas.save(AulaEntity.crear(
                        externa.codigoAula(),
                        externa.nombreAula(),
                        edificio,
                        externa.sillas(),
                        tipoAula,
                        dependencia,
                        true
                ));
            } else {
                aula.actualizar(
                        externa.nombreAula(),
                        externa.sillas(),
                        edificio,
                        tipoAula,
                        dependencia
                );
                aulas.save(aula);
            }
        }
        return aulasExternas.size();
    }

    private EdificioEntity resolverEdificio(Long codigo, String nombre, Map<Long, EdificioEntity> resueltos) {
        return resueltos.computeIfAbsent(codigo, c -> {
            var entity = edificios.findByCodigoSiga(c).orElseGet(() -> EdificioEntity.crear(c, nombre));
            entity.actualizarNombre(nombre);
            return edificios.save(entity);
        });
    }

    private TipoAulaEntity resolverTipoAula(Long codigo, String nombre, Map<Long, TipoAulaEntity> resueltos) {
        return resueltos.computeIfAbsent(codigo, c -> {
            var entity = tiposAula.findByCodigoSiga(c).orElseGet(() -> TipoAulaEntity.crear(c, nombre));
            entity.actualizarNombre(nombre);
            return tiposAula.save(entity);
        });
    }

    private DependenciaEntity resolverDependencia(Long codigo, String nombre, Map<Long, DependenciaEntity> resueltos) {
        return resueltos.computeIfAbsent(codigo, c -> {
            var entity = dependencias.findByCodigoSiga(c).orElseGet(() -> DependenciaEntity.crear(c, nombre));
            entity.actualizarNombre(nombre);
            return dependencias.save(entity);
        });
    }
}
