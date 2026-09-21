package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignaturaPracticaRepository extends JpaRepository<AsignaturaPractica, Integer> {

    List<AsignaturaPractica> findAllByOrderByOrdenAsc();
}
