package co.edu.uceva.aulasmart.users.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolJpaRepository extends JpaRepository<RolEntity, Short> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM RolEntity r WHERE r.nombre = 'ADMINISTRADOR'")
    Optional<RolEntity> bloquearAdministracion();

    Optional<RolEntity> findByNombreIgnoreCase(String nombre);
}
