package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.AmbitoPauta;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.TipoEvaluador;
import java.util.List;

/** Una pauta con sus dimensiones e indicadores en el orden de la pauta impresa, y su versión (HU-06). */
public record PautaDto(Integer id, Integer asignaturaId, String nombre, TipoEvaluador tipoEvaluador, AmbitoPauta ambito,
                       Integer numeroObservacion, Integer escalaMin, Integer escalaMax, boolean activa, int totalIndicadores,
                       int puntajeMaximo, List<DimensionDto> dimensiones, int version, Integer versionAnteriorId) {

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
        int total = p.indicadores().size();
        // HU-07, criterio 4: el puntaje máximo se calcula con los indicadores de la pauta (no se escribe a mano).
        return new PautaDto(p.getId(), p.getAsignatura().getId(), p.getNombre(), p.getTipoEvaluador(), p.getAmbito(),
                p.getNumeroObservacion(), p.getEscalaMin(), p.getEscalaMax(), Boolean.TRUE.equals(p.getActiva()), total,
                total * p.getEscalaMax(), dimensiones,
                p.getVersion(), p.getVersionAnterior() == null ? null : p.getVersionAnterior().getId());
    }
}
