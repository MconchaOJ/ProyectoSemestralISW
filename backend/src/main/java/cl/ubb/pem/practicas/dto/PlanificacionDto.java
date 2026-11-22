package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Planificacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Planificación completa, con su historial de revisiones en orden cronológico (HU-04, criterio 7), en el formato
 * oficial de la carrera (S-45). Del encabezado del formato, el profesor/a es {@code estudiante}, el año y el período
 * salen de {@code periodo} (ej. 2026-2) y {@code asignaturaEscolar} es siempre "Matemática"; {@code asignatura} es
 * la práctica (ej. Trabajo Colaborativo en el Aula).
 */
public record PlanificacionDto(Long id, Long inscripcionId, EstudianteResumenDto estudiante, String asignatura,
                               String periodo, String asignaturaEscolar, String titulo,
                               String establecimiento, String curso, String unidad, String objetivoAprendizaje,
                               String objetivoActitudinal, String objetivoClase, LocalDate fechaClase,
                               Integer duracionMinutos, String recursos, String instrumentosEvaluacion,
                               String inicio, Integer inicioMinutos, String desarrollo, Integer desarrolloMinutos,
                               String cierre, Integer cierreMinutos,
                               EstadoPlanificacion estado, String pendienteDe,
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
                insc.getAsignatura().getNombre(), insc.getPeriodo(), Planificacion.ASIGNATURA_ESCOLAR, p.getTitulo(),
                p.getEstablecimiento(), p.getCurso(), p.getUnidad(), p.getObjetivoAprendizaje(),
                p.getObjetivoActitudinal(), p.getObjetivoClase(), p.getFechaClase(),
                p.getDuracionMinutos(), p.getRecursos(), p.getInstrumentosEvaluacion(),
                p.getInicio(), p.getInicioMinutos(), p.getDesarrollo(), p.getDesarrolloMinutos(),
                p.getCierre(), p.getCierreMinutos(),
                p.getEstado(), pendienteDe(p.getEstado()), p.getFechaCreacion(),
                p.getFechaActualizacion(), p.getRevisiones().stream().map(RevisionDto::desde).toList());
    }
}
