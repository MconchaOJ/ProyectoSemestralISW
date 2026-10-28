package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import java.math.BigDecimal;
import java.util.List;

/**
 * Recorrido completo de un estudiante por sus prácticas. Además del historial entrega el contexto que usará el
 * chatbot en un sprint posterior: el semestre actual (en {@code estudiante}) y las asignaturas aprobadas.
 */
public record HistorialDto(EstudianteDto estudiante, List<AsignaturaAprobadaDto> asignaturasAprobadas,
                           List<InscripcionHistorialDto> inscripciones) {

    public record AsignaturaAprobadaDto(Integer asignaturaId, String codigo, String nombre, String periodo,
                                        BigDecimal notaFinal) {
    }

    /**
     * Una inscripción con sus evaluaciones FINALIZADAS y todas sus planificaciones (con su estado actual).
     * {@code equipo}: la dupla o el grupo con que trabajó (HU-10, criterio 3); nulo si fue individual.
     */
    public record InscripcionHistorialDto(Long inscripcionId, AsignaturaDto asignatura, String periodo,
                                          EstadoInscripcion estado, BigDecimal notaFinal,
                                          UsuarioResumenDto profesorGuia, UsuarioResumenDto profesorCurso,
                                          List<EvaluacionResumenDto> evaluaciones,
                                          List<PlanificacionResumenDto> planificaciones,
                                          EquipoResumenDto equipo) {
    }
}
