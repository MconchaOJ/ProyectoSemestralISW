package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.AmbitoPauta;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.RespuestaIndicador;
import cl.ubb.pem.practicas.entity.TipoEvaluador;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** Detalle completo de una evaluación, con el puntaje de cada indicador respondido. */
public record EvaluacionDto(Long id, Long inscripcionId, EstudianteResumenDto estudiante, PautaRef pauta,
                            UsuarioResumenDto evaluador, Long planificacionId, LocalDate fechaClase,
                            String establecimiento, String curso, LocalTime horaInicio, LocalTime horaTermino,
                            String objetivoClase, String observacionesGenerales, EstadoEvaluacion estado,
                            Integer puntajeTotal, Integer puntajeMaximo, BigDecimal promedio,
                            LocalDateTime fechaFinalizacion, int indicadoresRespondidos, int indicadoresTotales,
                            List<RespuestaDto> respuestas) {

    /** tipoEvaluador y numeroObservacion (HU-07): quién la aplica y si es la primera o la segunda observación. */
    public record PautaRef(Integer id, String nombre, Integer escalaMin, Integer escalaMax, TipoEvaluador tipoEvaluador,
                           AmbitoPauta ambito, Integer numeroObservacion) {
    }

    public record RespuestaDto(Integer indicadorId, Integer numero, String dimension, String descripcion,
                               Integer puntaje, String comentario) {

        static RespuestaDto desde(RespuestaIndicador r) {
            var ind = r.getIndicador();
            return new RespuestaDto(ind.getId(), ind.getNumero(), ind.getDimension().getNombre(), ind.getDescripcion(),
                    r.getPuntaje(), r.getComentario());
        }
    }

    public static EvaluacionDto desde(Evaluacion e) {
        var pauta = e.getPauta();
        int totalIndicadores = pauta.indicadores().size();
        List<RespuestaDto> respuestas = e.getRespuestas().stream()
                .map(RespuestaDto::desde)
                .sorted(Comparator.comparing(RespuestaDto::numero))
                .toList();
        return new EvaluacionDto(e.getId(), e.getInscripcion().getId(),
                EstudianteResumenDto.desde(e.getInscripcion().getEstudiante()),
                new PautaRef(pauta.getId(), pauta.getNombre(), pauta.getEscalaMin(), pauta.getEscalaMax(),
                        pauta.getTipoEvaluador(), pauta.getAmbito(), pauta.getNumeroObservacion()),
                UsuarioResumenDto.desde(e.getEvaluador()),
                e.getPlanificacion() == null ? null : e.getPlanificacion().getId(),
                e.getFechaClase(), e.getEstablecimiento(), e.getCurso(), e.getHoraInicio(), e.getHoraTermino(),
                e.getObjetivoClase(), e.getObservacionesGenerales(), e.getEstado(),
                e.getPuntajeTotal(), totalIndicadores * pauta.getEscalaMax(), e.getPromedio(),
                e.getFechaFinalizacion(), respuestas.size(), totalIndicadores, respuestas);
    }
}
