package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);

    List<Rol> findAllByOrderByIdAsc();
}
