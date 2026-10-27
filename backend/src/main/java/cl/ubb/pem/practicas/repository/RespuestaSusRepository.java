package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.RespuestaSus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaSusRepository extends JpaRepository<RespuestaSus, Long> {

    Optional<RespuestaSus> findByValidadorId(Long estudianteId);

    List<RespuestaSus> findAllByOrderByFechaAscIdAsc();
}
