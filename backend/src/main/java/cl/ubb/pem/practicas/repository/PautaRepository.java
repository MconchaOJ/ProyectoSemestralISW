package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.Pauta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PautaRepository extends JpaRepository<Pauta, Integer> {

    List<Pauta> findByAsignaturaIdAndActivaTrueOrderByIdAsc(Integer asignaturaId);

    List<Pauta> findByActivaTrueOrderByIdAsc();
}
