package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Una fila de la cartola de prácticas (HU-31): una inscripción cerrada con su resultado y el resumen de sus
 * evaluaciones finalizadas. {@code promedioEvaluaciones} es el promedio de los promedios de esas evaluaciones (nulo
 * si no tiene ninguna); {@code profesorGuia} es nulo en el ciclo inicial, que no va a un colegio.
 */
public record CartolaFilaDto(Long inscripcionId, String estudiante, String rut, String asignatura, String periodo,
                             String profesorGuia, String profesorCurso, EstadoInscripcion estado,
                             LocalDate fechaCierre, BigDecimal notaFinal,
                             int evaluacionesFinalizadas, BigDecimal promedioEvaluaciones) {

    public static CartolaFilaDto desde(Inscripcion i, List<BigDecimal> promediosFinalizadas) {
        List<BigDecimal> promedios = promediosFinalizadas.stream().filter(Objects::nonNull).toList();
        BigDecimal promedio = promedios.isEmpty() ? null
                : promedios.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(promedios.size()), 2, RoundingMode.HALF_UP);
        return new CartolaFilaDto(i.getId(), i.getEstudiante().getUsuario().getNombreCompleto(),
                i.getEstudiante().getRut(), i.getAsignatura().getNombre(), i.getPeriodo(),
                i.getProfesorGuia() == null ? null : i.getProfesorGuia().getNombreCompleto(),
                i.getProfesorCurso().getNombreCompleto(), i.getEstado(), i.getFechaCierre(), i.getNotaFinal(),
                promedios.size(), promedio);
    }
}
