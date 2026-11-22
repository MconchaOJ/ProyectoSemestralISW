package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.LecturaChat;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LecturaChatRepository extends JpaRepository<LecturaChat, Long> {

    /** La última lectura de esta conversación hecha con un rol (JEFE_CARRERA o COORDINADOR; HU-20, criterio 3). */
    Optional<LecturaChat> findFirstByInscripcionIdAndRolOrderByFechaDescIdDesc(Long inscripcionId, String rol);

    long countByInscripcionId(Long inscripcionId);

    long countByInscripcionIdAndRol(Long inscripcionId, String rol);
}
