package cl.ubb.pem.practicas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-14: panel de monitoreo del coordinador y del jefe de carrera. Una fila por asignatura y periodo con el avance, y
 * la lista de lo que "requiere atención": planificaciones que esperan revisión más de {@code diasAtencion} días
 * (SUPUESTO S-79) y estudiantes en curso sin ninguna evaluación finalizada.
 */
public record PanelDto(LocalDateTime generado, Integer asignaturaId, String periodo, int diasAtencion,
                       Totales totales, List<Fila> filas, List<PlanificacionEsperando> planificacionesEsperando,
                       List<SinEvaluar> sinEvaluar) {

    /** Suma de todas las filas. */
    public record Totales(int inscritos, int enCurso, int planificacionesEnRevisionGuia, int planificacionesEnAutorizacion,
                          int evaluacionesFinalizadas, int estudiantesSinEvaluar) {
    }

    /**
     * Avance de una asignatura en un periodo. {@code estudiantesSinEvaluar} cuenta solo en las prácticas que van a un
     * colegio (las únicas con profesor guía que evalúe con pauta); en las demás es nulo.
     */
    public record Fila(Integer asignaturaId, String asignatura, int orden, String periodo, int inscritos, int enCurso,
                       int planificacionesEnRevisionGuia, int planificacionesEnAutorizacion, int evaluacionesFinalizadas,
                       Integer estudiantesSinEvaluar) {
    }

    /** Una planificación que espera al guía o al profesor del curso hace más días que el umbral. */
    public record PlanificacionEsperando(Long planificacionId, String titulo, Long inscripcionId, String estudiante,
                                         String asignatura, String periodo, String esperaA, String rolQueDebeActuar,
                                         LocalDate fechaClase, long diasEsperando) {
    }

    /** Un estudiante con la práctica en curso y ninguna evaluación finalizada. */
    public record SinEvaluar(Long inscripcionId, Long estudianteId, String estudiante, String asignatura, String periodo,
                             String profesorGuia) {
    }
}
