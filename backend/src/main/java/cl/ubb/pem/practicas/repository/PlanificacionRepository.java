package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Planificacion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanificacionRepository extends JpaRepository<Planificacion, Long> {

    /** Listado con filtros opcionales y alcance por rol (parámetro nulo = sin filtro). */
    @Query("""
            select p from Planificacion p
            where (:inscripcionId is null or p.inscripcion.id = :inscripcionId)
              and (:estado is null or p.estado = :estado)
              and (:guiaId is null or p.inscripcion.profesorGuia.id = :guiaId)
              and (:cursoId is null or p.inscripcion.profesorCurso.id = :cursoId)
              and (:usuarioEstudianteId is null or p.inscripcion.estudiante.usuario.id = :usuarioEstudianteId)
            order by p.fechaActualizacion desc, p.id desc""")
    List<Planificacion> buscar(@Param("inscripcionId") Long inscripcionId,
                               @Param("estado") EstadoPlanificacion estado,
                               @Param("guiaId") Long guiaId,
                               @Param("cursoId") Long cursoId,
                               @Param("usuarioEstudianteId") Long usuarioEstudianteId);

    @Query("""
            select p from Planificacion p
            where p.inscripcion.id in :inscripcionIds
            order by p.fechaClase, p.id""")
    List<Planificacion> findPorInscripciones(@Param("inscripcionIds") Collection<Long> inscripcionIds);
}
