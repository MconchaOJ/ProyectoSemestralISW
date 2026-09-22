package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.DocumentoPlanificacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoPlanificacionRepository extends JpaRepository<DocumentoPlanificacion, Long> {

    List<DocumentoPlanificacion> findByPlanificacionIdOrderByFechaSubidaAsc(Long planificacionId);
}
