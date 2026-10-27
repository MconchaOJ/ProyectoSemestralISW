package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.LecturaChatJefeCarrera;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LecturaChatJefeCarreraRepository extends JpaRepository<LecturaChatJefeCarrera, Long> {

    /** La última lectura del jefe de carrera en esta conversación (HU-20, criterio 3). */
    Optional<LecturaChatJefeCarrera> findFirstByInscripcionIdOrderByFechaDescIdDesc(Long inscripcionId);

    long countByInscripcionId(Long inscripcionId);
}
