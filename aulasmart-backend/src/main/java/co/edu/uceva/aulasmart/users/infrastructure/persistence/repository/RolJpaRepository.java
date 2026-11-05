package co.edu.uceva.aulasmart.users.infrastructure.persistence.repository;

import co.edu.uceva.aulasmart.users.infrastructure.persistence.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolJpaRepository extends JpaRepository<RolEntity, Short> {

    Optional<RolEntity> findByNombreIgnoreCase(String nombre);
}
