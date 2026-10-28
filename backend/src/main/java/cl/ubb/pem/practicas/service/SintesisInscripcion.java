package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.EvaluacionResumenDto;
import cl.ubb.pem.practicas.dto.HistorialDto.InscripcionHistorialDto;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Síntesis de una inscripción para los informes (HU-08) y el historial en PDF (HU-28): cuántas evaluaciones
 * finalizadas tiene y el promedio de sus promedios (como la cartola, HU-31), y cuántas planificaciones y cuántas de
 * ellas quedaron autorizadas.
 */
record SintesisInscripcion(int evaluaciones, BigDecimal promedioEvaluaciones, int planificaciones,
                           int planificacionesAutorizadas) {

    static SintesisInscripcion de(InscripcionHistorialDto i) {
        int autorizadas = (int) i.planificaciones().stream()
                .filter(p -> p.estado() == EstadoPlanificacion.AUTORIZADA).count();
        List<BigDecimal> promedios = i.evaluaciones().stream().map(EvaluacionResumenDto::promedio).toList();
        return new SintesisInscripcion(i.evaluaciones().size(), promedio(promedios), i.planificaciones().size(),
                autorizadas);
    }

    /** Promedio con 2 decimales de los valores no nulos; nulo si no hay ninguno. */
    static BigDecimal promedio(List<BigDecimal> valores) {
        List<BigDecimal> presentes = valores.stream().filter(Objects::nonNull).toList();
        if (presentes.isEmpty()) {
            return null;
        }
        return presentes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(presentes.size()), 2, RoundingMode.HALF_UP);
    }
}
