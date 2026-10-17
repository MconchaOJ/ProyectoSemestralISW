package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.Pauta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PautaRepository extends JpaRepository<Pauta, Integer> {

    List<Pauta> findByAsignaturaIdAndActivaTrueOrderByIdAsc(Integer asignaturaId);

    List<Pauta> findByActivaTrueOrderByIdAsc();

    /** HU-06: todas las versiones, para la pantalla del coordinador. */
    List<Pauta> findAllByOrderByAsignaturaOrdenAscNombreAscVersionAsc();

    List<Pauta> findByAsignaturaIdOrderByNombreAscVersionAsc(Integer asignaturaId);

    List<Pauta> findByAsignaturaIdAndNombreIgnoreCase(Integer asignaturaId, String nombre);

    /** ¿Ya existe una versión siguiente? Si existe, esta no es la vigente y no se edita. */
    Optional<Pauta> findByVersionAnteriorId(Integer versionAnteriorId);
}
