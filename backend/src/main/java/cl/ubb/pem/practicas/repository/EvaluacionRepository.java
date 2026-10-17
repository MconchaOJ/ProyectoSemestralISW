package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    /** Evaluaciones (borradores incluidos) hechas con una pauta: si hay alguna, la pauta ya no se edita (HU-06). */
    long countByPautaId(Integer pautaId);

    /** Listado con filtros opcionales y alcance por rol (parámetro nulo = sin filtro). */
    @Query("""
            select e from Evaluacion e
            where (:inscripcionId is null or e.inscripcion.id = :inscripcionId)
              and (:estado is null or e.estado = :estado)
              and (:guiaId is null or e.inscripcion.profesorGuia.id = :guiaId)
              and (:cursoId is null or e.inscripcion.profesorCurso.id = :cursoId)
              and (:usuarioEstudianteId is null or e.inscripcion.estudiante.usuario.id = :usuarioEstudianteId)
            order by e.fechaClase desc, e.id desc""")
    List<Evaluacion> buscar(@Param("inscripcionId") Long inscripcionId,
                            @Param("estado") EstadoEvaluacion estado,
                            @Param("guiaId") Long guiaId,
                            @Param("cursoId") Long cursoId,
                            @Param("usuarioEstudianteId") Long usuarioEstudianteId);

    @Query("""
            select e from Evaluacion e
            where e.inscripcion.id in :inscripcionIds and e.estado = :estado
            order by e.fechaClase, e.id""")
    List<Evaluacion> findPorInscripciones(@Param("inscripcionIds") Collection<Long> inscripcionIds,
                                          @Param("estado") EstadoEvaluacion estado);
}
