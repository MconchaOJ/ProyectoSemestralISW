package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-14: panel de monitoreo del coordinador y del jefe de carrera. Una fila por asignatura y periodo con el avance, y
 * la lista de lo que "requiere atención": planificaciones que esperan revisión más de {@code diasAtencion} días
 * (SUPUESTO S-79) y estudiantes en curso sin ninguna evaluación finalizada.
 * Integrado del trabajo de Alej (commit 7e9b132): todas las planificaciones pendientes con a quién esperan y hace
 * cuántos días ({@code pendientes}, también las devueltas que esperan al estudiante), el avance por estudiante
 * ({@code avance}) y los conteos de devueltas, autorizadas y borradores de evaluación.
 */
public record PanelDto(LocalDateTime generado, Integer asignaturaId, String periodo, int diasAtencion,
                       Totales totales, List<Fila> filas, List<PlanificacionEsperando> planificacionesEsperando,
                       List<SinEvaluar> sinEvaluar, List<Pendiente> pendientes, List<Avance> avance) {

    /** Suma de todas las filas. */
    public record Totales(int inscritos, int enCurso, int planificacionesEnRevisionGuia, int planificacionesEnAutorizacion,
                          int evaluacionesFinalizadas, int estudiantesSinEvaluar, int planificacionesConObservaciones,
                          int evaluacionesBorrador) {
    }

    /**
     * Avance de una asignatura en un periodo. {@code estudiantesSinEvaluar} cuenta solo en las prácticas que van a un
     * colegio (las únicas con profesor guía que evalúe con pauta); en las demás es nulo.
     */
    public record Fila(Integer asignaturaId, String asignatura, int orden, String periodo, int inscritos, int enCurso,
                       int planificacionesEnRevisionGuia, int planificacionesEnAutorizacion, int evaluacionesFinalizadas,
                       Integer estudiantesSinEvaluar, int planificacionesConObservaciones, int planificacionesAutorizadas,
                       int evaluacionesBorrador, BigDecimal promedioEvaluaciones) {
    }

    /** Una planificación que espera al guía o al profesor del curso hace más días que el umbral. */
    public record PlanificacionEsperando(Long planificacionId, String titulo, Long inscripcionId, String estudiante,
                                         String asignatura, String periodo, String esperaA, String rolQueDebeActuar,
                                         LocalDate fechaClase, long diasEsperando) {
    }

    /** Un estudiante con la práctica en curso y ninguna evaluación finalizada ({@code borradores}: las empezadas). */
    public record SinEvaluar(Long inscripcionId, Long estudianteId, String estudiante, String asignatura, String periodo,
                             String profesorGuia, int borradores) {
    }

    /**
     * Toda planificación que espera a alguien: al guía (EN_REVISION_GUIA), al profesor del curso (EN_AUTORIZACION) o al
     * estudiante (CON_OBSERVACIONES). {@code requiereAtencion}: espera a un profesor hace más días que el umbral (S-79).
     */
    public record Pendiente(Long planificacionId, String titulo, Long inscripcionId, String estudiante, String asignatura,
                            String periodo, String estado, String esperaA, String rolQueDebeActuar, LocalDate fechaClase,
                            long diasEsperando, boolean requiereAtencion) {
    }

    /** El avance de cada estudiante en cada inscripción del filtro. */
    public record Avance(Long inscripcionId, Long estudianteId, String estudiante, String asignatura, String periodo,
                         EstadoInscripcion estado, int planificaciones, int planificacionesAutorizadas,
                         int evaluacionesFinalizadas, BigDecimal promedioEvaluaciones, BigDecimal notaFinal) {
    }
}
