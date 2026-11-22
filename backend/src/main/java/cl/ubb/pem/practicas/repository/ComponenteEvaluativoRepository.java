package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.ComponenteEvaluativo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponenteEvaluativoRepository extends JpaRepository<ComponenteEvaluativo, Integer> {

    List<ComponenteEvaluativo> findByAsignaturaIdOrderByOrdenAsc(Integer asignaturaId);

    /** Una pauta es parte de, como máximo, un componente (uq_componente_asignatura_pauta). */
    Optional<ComponenteEvaluativo> findByPautaId(Integer pautaId);
}
