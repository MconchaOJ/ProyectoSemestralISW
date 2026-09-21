package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.Estudiante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Optional<Estudiante> findByUsuarioId(Long usuarioId);

    boolean existsByRut(String rut);

    /** Busca por nombre, apellidos o RUT. La colación de la base ignora mayúsculas y tildes. Con q vacío trae todos. */
    @Query("""
            select e from Estudiante e join fetch e.usuario u
            where u.nombre like concat('%', :q, '%')
               or u.apellidos like concat('%', :q, '%')
               or concat(u.nombre, ' ', u.apellidos) like concat('%', :q, '%')
               or e.rut like concat('%', :q, '%')
            order by u.apellidos, u.nombre""")
    List<Estudiante> buscar(@Param("q") String q);
}
