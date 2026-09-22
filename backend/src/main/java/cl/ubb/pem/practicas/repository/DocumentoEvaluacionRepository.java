package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.DocumentoEvaluacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoEvaluacionRepository extends JpaRepository<DocumentoEvaluacion, Long> {

    List<DocumentoEvaluacion> findByEvaluacionIdOrderByFechaSubidaAsc(Long evaluacionId);
}
