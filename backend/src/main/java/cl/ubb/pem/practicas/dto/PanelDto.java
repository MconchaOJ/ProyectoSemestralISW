package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Panel de monitoreo del coordinador (HU-14): el avance de cada asignatura y de cada estudiante con los filtros
 * pedidos (periodo y asignatura, ambos opcionales). Criterio 1: planificaciones pendientes, evaluaciones hechas y
 * estudiantes sin evaluar.
 */
public record PanelDto(String periodo, Integer asignaturaId, ResumenDto resumen, List<AsignaturaPanelDto> asignaturas,
                       List<PendienteDto> planificacionesPendientes, List<SinEvaluarDto> sinEvaluar,
                       List<AvanceDto> avance) {

    /**
     * Cifras del filtro. Pendientes = enviadas y esperando al guía o al profesor del curso; con observaciones =
     * devueltas, esperando al estudiante; las tres solo de inscripciones EN_CURSO. Sin evaluar: ver
     * {@link SinEvaluarDto}.
     */
    public record ResumenDto(int inscripciones, int enCurso, int pendientesGuia, int pendientesCurso,
                             int conObservaciones, int evaluacionesFinalizadas, int evaluacionesBorrador,
                             int estudiantesSinEvaluar) {
    }

    /** Una fila por asignatura con inscripciones en el filtro, en el orden de la malla. */
    public record AsignaturaPanelDto(Integer asignaturaId, String nombre, int orden, int inscripciones, int enCurso,
                                     int pendientesGuia, int pendientesCurso, int conObservaciones, int autorizadas,
                                     int evaluacionesFinalizadas, int evaluacionesBorrador, int sinEvaluar,
                                     BigDecimal promedioEvaluaciones) {
    }

    /** Una planificación que espera a alguien: {@code esperaA} es el nombre del guía o del profesor, o "Estudiante". */
    public record PendienteDto(Long planificacionId, Long inscripcionId, String estudiante, String asignatura,
                               String titulo, LocalDate fechaClase, EstadoPlanificacion estado, String esperaA,
                               long diasEsperando) {
    }

    /**
     * Una inscripción EN_CURSO sin ninguna evaluación FINALIZADA, en una asignatura que tiene pauta en el sistema
     * (SUPUESTO S-81). {@code borradores}: evaluaciones empezadas y aún no finalizadas.
     */
    public record SinEvaluarDto(Long inscripcionId, Long estudianteId, String estudiante, String asignatura,
                                String periodo, String profesorGuia, int borradores) {
    }

    /** El avance de cada estudiante en cada inscripción del filtro. */
    public record AvanceDto(Long inscripcionId, Long estudianteId, String estudiante, String asignatura,
                            String periodo, EstadoInscripcion estado, int planificaciones,
                            int planificacionesAutorizadas, int evaluacionesFinalizadas,
                            BigDecimal promedioEvaluaciones, BigDecimal notaFinal, BigDecimal notaCalculada) {
    }
}
