package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.RespuestaRubricaExperto;
import cl.ubb.pem.practicas.entity.RespuestaSus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Los resultados de la validación del chatbot para el coordinador (HU-30): el resumen de cada instrumento (promedios)
 * y cada respuesta, con quién y cuándo respondió. Los promedios son nulos si nadie ha respondido.
 */
public record ResultadosValidacionDto(InstrumentosValidacionDto instrumentos, ResumenSus sus, ResumenRubrica rubrica) {

    /** @param promedioPuntaje promedio del puntaje SUS (0 a 100; 68 es el promedio de referencia de la escala) */
    public record ResumenSus(int respuestas, BigDecimal promedioPuntaje, List<BigDecimal> promedioPorItem,
                             List<RespuestaSusDto> detalle) {
    }

    public record RespuestaSusDto(EstudianteResumenDto validador, LocalDateTime fecha, List<Integer> items,
                                  BigDecimal puntaje) {

        public static RespuestaSusDto desde(RespuestaSus r) {
            return new RespuestaSusDto(EstudianteResumenDto.desde(r.getValidador()), r.getFecha(), r.getItems(),
                    r.getPuntaje());
        }
    }

    public record ResumenRubrica(int respuestas, List<BigDecimal> promedioPorCriterio, List<RespuestaRubricaDto> detalle) {
    }

    public record RespuestaRubricaDto(EstudianteResumenDto validador, LocalDateTime fecha, List<CriterioRespondido> criterios,
                                      String fortalezas, String debilidades, String mejoras, String recomendacion) {

        public static RespuestaRubricaDto desde(RespuestaRubricaExperto r) {
            return new RespuestaRubricaDto(EstudianteResumenDto.desde(r.getValidador()), r.getFecha(),
                    r.getCriterios().stream()
                            .map(c -> new CriterioRespondido(c.getNumero(), c.getPuntaje(), c.getComentario())).toList(),
                    r.getFortalezas(), r.getDebilidades(), r.getMejoras(), r.getRecomendacion());
        }
    }

    public record CriterioRespondido(int numero, int puntaje, String comentario) {
    }
}
