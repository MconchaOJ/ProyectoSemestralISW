package cl.ubb.pem.practicas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-08, criterio 2: informe de una asignatura en un período (solo lectura, para el coordinador y el jefe de carrera):
 * inscripciones y notas finales, evaluaciones finalizadas por pauta con su distribución, planificaciones y una fila por
 * estudiante. Los promedios van con 2 decimales; nulos si no hay datos.
 */
public record InformeAsignaturaDto(Integer asignaturaId, String asignaturaCodigo, String asignatura, String periodo,
                                   LocalDateTime generado, Inscripciones inscripciones, Notas notasFinales,
                                   List<PorPauta> evaluaciones, Planificaciones planificaciones,
                                   List<FilaEstudiante> estudiantes) {

    /** Cantidad de inscritos y cuántos hay en cada estado. */
    public record Inscripciones(int total, int enCurso, int aprobadas, int reprobadas) {
    }

    /** Estadística de un conjunto de notas (escala chilena) y su distribución en tramos. */
    public record Notas(int cantidad, BigDecimal promedio, BigDecimal minimo, BigDecimal maximo, List<Tramo> tramos) {
    }

    /** Un tramo de la distribución, por ejemplo "5,0 a 5,9", con cuántos casos caen en él. */
    public record Tramo(String tramo, int cantidad) {
    }

    /**
     * Evaluaciones finalizadas con una misma pauta (y versión): puntaje promedio, porcentaje de logro sobre el máximo,
     * estadística y distribución del promedio de cada evaluación, y el puntaje promedio por indicador en cada dimensión.
     */
    public record PorPauta(Integer pautaId, String pauta, int version, String tipoEvaluador, int evaluaciones,
                           int puntajeMaximo, BigDecimal puntajePromedio, BigDecimal porcentajeLogro,
                           Notas promedios, List<PorDimension> dimensiones) {
    }

    /** Puntaje promedio por indicador en una dimensión de la pauta (en la escala de la pauta). */
    public record PorDimension(String dimension, BigDecimal promedioPorIndicador) {
    }

    /** Planificaciones de la asignatura y período: por estado y cuántas fueron devueltas al menos una vez. */
    public record Planificaciones(int total, int borrador, int enRevisionGuia, int conObservaciones,
                                  int enAutorizacion, int autorizadas, int devueltasAlMenosUnaVez) {
    }

    /** Una fila por estudiante inscrito. */
    public record FilaEstudiante(Long estudianteId, String estudiante, String rut, String estado, BigDecimal notaFinal,
                                 int evaluacionesFinalizadas, BigDecimal promedioEvaluaciones, int planificaciones,
                                 int planificacionesAutorizadas) {
    }
}
