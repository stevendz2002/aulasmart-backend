package co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.adapter;

import co.edu.uceva.aulasmart.solicitudes.application.port.SolicitudReservaPort;
import co.edu.uceva.aulasmart.solicitudes.domain.model.SolicitudReserva;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudNoEncontradaException;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.entity.SolicitudAulaEntity;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.mapper.SolicitudAulaMapper;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.repository.SolicitudAulaJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class SolicitudReservaPersistenceAdapter implements SolicitudReservaPort {

    private final SolicitudAulaJpaRepository repository;

    public SolicitudReservaPersistenceAdapter(SolicitudAulaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SolicitudReserva guardar(SolicitudReserva solicitud) {
        if (solicitud.id() == null) {
            var entity = SolicitudAulaEntity.nueva(
                            solicitud.docenteId(),
                            solicitud.aulaId(),
                            solicitud.intervalo().inicio(),
                            solicitud.intervalo().fin(),
                            solicitud.estado(),
                            solicitud.motivo(),
                            solicitud.creadaEn()
                    );
            return SolicitudAulaMapper.aDominio(repository.save(entity));
        }
        var entity = repository.findById(solicitud.id())
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitud.id()));
        entity.revisar(solicitud.estado(), solicitud.revisadaPorId(), solicitud.revisadaEn());
        return SolicitudAulaMapper.aDominio(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolicitudReserva> buscarPorId(Long idSolicitud) {
        return repository.findById(idSolicitud).map(SolicitudAulaMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudReserva> buscarPorDocente(Long docenteId) {
        return repository.findByDocenteIdOrderByCreadaEnDesc(docenteId).stream()
                .map(SolicitudAulaMapper::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudReserva> buscarTodas() {
        return repository.findAllByOrderByCreadaEnDesc().stream()
                .map(SolicitudAulaMapper::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudReserva> buscarPorEstado(String estado) {
        return repository.findByEstadoOrderByCreadaEnDesc(estado).stream()
                .map(SolicitudAulaMapper::aDominio)
                .toList();
    }
}
