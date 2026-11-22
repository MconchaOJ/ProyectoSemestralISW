package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Una evaluación en listados y en el historial: sin el detalle de cada indicador. La nota es de HU-09 (S-81). */
public record EvaluacionResumenDto(Long id, Long inscripcionId, Integer pautaId, String pautaNombre, LocalDate fechaClase,
                                   String establecimiento, String curso, EstadoEvaluacion estado, Integer puntajeTotal,
                                   BigDecimal promedio, BigDecimal nota) {

    public static EvaluacionResumenDto desde(Evaluacion e) {
        return new EvaluacionResumenDto(e.getId(), e.getInscripcion().getId(), e.getPauta().getId(), e.getPauta().getNombre(),
                e.getFechaClase(), e.getEstablecimiento(), e.getCurso(), e.getEstado(), e.getPuntajeTotal(), e.getPromedio(),
                e.getNota());
    }
}
