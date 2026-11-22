package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.NotaComponenteExterno;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaComponenteExternoRepository extends JpaRepository<NotaComponenteExterno, Long> {

    List<NotaComponenteExterno> findByInscripcionIdIn(Collection<Long> inscripcionIds);

    Optional<NotaComponenteExterno> findByInscripcionIdAndComponenteId(Long inscripcionId, Integer componenteId);

    long countByComponenteId(Integer componenteId);

    void deleteByComponenteId(Integer componenteId);
}
