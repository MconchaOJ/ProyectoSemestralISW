package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.NotaVoz;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaVozRepository extends JpaRepository<NotaVoz, Long> {

    Optional<NotaVoz> findByEvaluacionId(Long evaluacionId);
}
