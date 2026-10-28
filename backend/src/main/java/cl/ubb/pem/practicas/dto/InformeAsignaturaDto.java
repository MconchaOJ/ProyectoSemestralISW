package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import java.math.BigDecimal;
import java.util.List;

/**
 * Informe de una asignatura en un periodo (HU-08, criterio 2): el resumen de sus inscripciones, la distribución de
 * las notas finales y de los puntajes de cada pauta usada, y una fila por estudiante. Solo cuenta evaluaciones
 * FINALIZADAS (los borradores no tienen puntaje).
 */
public record InformeAsignaturaDto(AsignaturaDto asignatura, String periodo, ResumenDto resumen,
                                   List<TramoDto> distribucionNotasFinales, List<PautaUsadaDto> pautas,
                                   List<FilaEstudianteDto> estudiantes) {

    /** {@code promedioNotaFinal} solo de las cerradas con nota; {@code promedioEvaluaciones} de todos los promedios. */
    public record ResumenDto(int inscripciones, int enCurso, int aprobadas, int reprobadas,
                             BigDecimal promedioNotaFinal, int evaluacionesFinalizadas,
                             BigDecimal promedioEvaluaciones, int planificaciones, int planificacionesAutorizadas) {
    }

    /** Un tramo de una distribución, por ejemplo "4,0 a 4,9" con 3 inscripciones. */
    public record TramoDto(String tramo, int cantidad) {
    }

    /**
     * Una pauta (en la versión que se usó) con sus evaluaciones finalizadas del periodo. La distribución va por
     * porcentaje del puntaje máximo, así se comparan pautas con escalas distintas (1 a 7, 2 a 7...). SUPUESTO S-77.
     */
    public record PautaUsadaDto(Integer pautaId, String nombre, int version, int puntajeMaximo, int evaluaciones,
                                BigDecimal puntajePromedio, Integer puntajeMinimo, Integer puntajeMasAlto,
                                BigDecimal promedio, List<TramoDto> distribucion) {
    }

    public record FilaEstudianteDto(Long inscripcionId, String estudiante, String rut, String profesorGuia,
                                    EstadoInscripcion estado, BigDecimal notaFinal, int evaluacionesFinalizadas,
                                    BigDecimal promedioEvaluaciones, int planificaciones,
                                    int planificacionesAutorizadas) {
    }
}
