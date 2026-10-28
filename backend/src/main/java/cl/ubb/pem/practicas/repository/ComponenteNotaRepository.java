package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.ComponenteNota;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponenteNotaRepository extends JpaRepository<ComponenteNota, Integer> {

    /** La estructura evaluativa de una asignatura, en su orden (HU-09). */
    List<ComponenteNota> findByAsignaturaIdOrderByOrdenAscIdAsc(Integer asignaturaId);
}
