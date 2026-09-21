package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.AmbitoPauta;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.TipoEvaluador;
import java.util.List;

/** Una pauta con sus dimensiones e indicadores en el orden de la pauta impresa. */
public record PautaDto(Integer id, Integer asignaturaId, String nombre, TipoEvaluador tipoEvaluador, AmbitoPauta ambito,
                       Integer escalaMin, Integer escalaMax, boolean activa, int totalIndicadores,
                       List<DimensionDto> dimensiones) {

    public record DimensionDto(Integer id, String nombre, Integer orden, List<IndicadorDto> indicadores) {
    }

    public record IndicadorDto(Integer id, Integer numero, String descripcion) {
    }

    public static PautaDto desde(Pauta p) {
        List<DimensionDto> dimensiones = p.getDimensiones().stream()
                .map(d -> new DimensionDto(d.getId(), d.getNombre(), d.getOrden(),
                        d.getIndicadores().stream()
                                .map(i -> new IndicadorDto(i.getId(), i.getNumero(), i.getDescripcion())).toList()))
                .toList();
        return new PautaDto(p.getId(), p.getAsignatura().getId(), p.getNombre(), p.getTipoEvaluador(), p.getAmbito(),
                p.getEscalaMin(), p.getEscalaMax(), Boolean.TRUE.equals(p.getActiva()), p.indicadores().size(), dimensiones);
    }
}
