package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.Inscripcion;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    boolean existsByEstudianteIdAndAsignaturaIdAndPeriodo(Long estudianteId, Integer asignaturaId, String periodo);

    boolean existsByEstudianteIdAndProfesorGuiaId(Long estudianteId, Long profesorGuiaId);

    boolean existsByEstudianteIdAndProfesorCursoId(Long estudianteId, Long profesorCursoId);

    /** HU-07: el tutor ve el historial de un estudiante del que es tutor en alguna inscripción (como S-21). */
    boolean existsByEstudianteIdAndTutorId(Long estudianteId, Long tutorId);

    /** Recorrido cronológico de un estudiante: por periodo ("2025-2" ordena bien como texto) y luego por orden de práctica. */
    @Query("""
            select i from Inscripcion i
            where i.estudiante.id = :estudianteId
            order by i.periodo, i.asignatura.orden""")
    List<Inscripcion> findRecorrido(@Param("estudianteId") Long estudianteId);

    /**
     * Cartola de prácticas (HU-31): inscripciones cerradas entre dos fechas, ambas incluidas. Las EN_CURSO no tienen
     * fecha de cierre, así que nunca entran. Trae de una vez estudiante, asignatura y profesores (van en cada fila).
     */
    @Query("""
            select i from Inscripcion i
              join fetch i.estudiante e
              join fetch e.usuario
              join fetch i.asignatura
              left join fetch i.profesorGuia
              join fetch i.profesorCurso
            where i.fechaCierre between :desde and :hasta
            order by i.fechaCierre, i.periodo, i.asignatura.orden, e.usuario.apellidos, i.id""")
    List<Inscripcion> findCerradasEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * Listado con filtros opcionales (parámetro nulo = sin filtro). Los tres últimos parámetros son el alcance
     * que fija el servicio según el rol de quien consulta.
     */
    @Query("""
            select i from Inscripcion i
            where (:estudianteId is null or i.estudiante.id = :estudianteId)
              and (:asignaturaId is null or i.asignatura.id = :asignaturaId)
              and (:periodo is null or i.periodo = :periodo)
              and (:guiaId is null or i.profesorGuia.id = :guiaId)
              and (:cursoId is null or i.profesorCurso.id = :cursoId)
              and (:usuarioEstudianteId is null or i.estudiante.usuario.id = :usuarioEstudianteId)
            order by i.periodo desc, i.asignatura.orden, i.id""")
    List<Inscripcion> buscar(@Param("estudianteId") Long estudianteId,
                             @Param("asignaturaId") Integer asignaturaId,
                             @Param("periodo") String periodo,
                             @Param("guiaId") Long guiaId,
                             @Param("cursoId") Long cursoId,
                             @Param("usuarioEstudianteId") Long usuarioEstudianteId);
}
