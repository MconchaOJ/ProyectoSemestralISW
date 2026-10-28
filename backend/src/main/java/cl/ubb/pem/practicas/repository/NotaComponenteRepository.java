package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.NotaComponente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaComponenteRepository extends JpaRepository<NotaComponente, Long> {

    List<NotaComponente> findByInscripcionId(Long inscripcionId);

    Optional<NotaComponente> findByInscripcionIdAndComponenteId(Long inscripcionId, Integer componenteId);

    /** Si un componente ya tiene notas, no se borra (HU-09). */
    boolean existsByComponenteId(Integer componenteId);
}
