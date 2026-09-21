package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Planificacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Planificación completa, con su historial de revisiones en orden cronológico (HU-04, criterio 7). */
public record PlanificacionDto(Long id, Long inscripcionId, EstudianteResumenDto estudiante, String asignatura,
                               String periodo, String titulo, LocalDate fechaClase, String objetivo, String recursos,
                               String tiempos, Integer duracionMinutos, String evaluacionAprendizaje,
                               String interaccionParticipantes, EstadoPlanificacion estado, String pendienteDe,
                               LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion,
                               List<RevisionDto> revisiones) {

    /** Quién debe actuar ahora según el estado (nulo cuando ya está autorizada). */
    static String pendienteDe(EstadoPlanificacion estado) {
        return switch (estado) {
            case BORRADOR, CON_OBSERVACIONES -> "ESTUDIANTE";
            case EN_REVISION_GUIA -> "PROFESOR_GUIA";
            case EN_AUTORIZACION -> "PROFESOR_CURSO";
            case AUTORIZADA -> null;
        };
    }

    public static PlanificacionDto desde(Planificacion p) {
        var insc = p.getInscripcion();
        return new PlanificacionDto(p.getId(), insc.getId(), EstudianteResumenDto.desde(insc.getEstudiante()),
                insc.getAsignatura().getNombre(), insc.getPeriodo(), p.getTitulo(), p.getFechaClase(), p.getObjetivo(),
                p.getRecursos(), p.getTiempos(), p.getDuracionMinutos(), p.getEvaluacionAprendizaje(),
                p.getInteraccionParticipantes(), p.getEstado(), pendienteDe(p.getEstado()), p.getFechaCreacion(),
                p.getFechaActualizacion(), p.getRevisiones().stream().map(RevisionDto::desde).toList());
    }
}
