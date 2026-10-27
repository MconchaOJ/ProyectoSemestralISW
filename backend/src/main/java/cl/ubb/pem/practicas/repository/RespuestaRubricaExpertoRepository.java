package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.RespuestaRubricaExperto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaRubricaExpertoRepository extends JpaRepository<RespuestaRubricaExperto, Long> {

    Optional<RespuestaRubricaExperto> findByValidadorId(Long estudianteId);

    List<RespuestaRubricaExperto> findAllByOrderByFechaAscIdAsc();
}
