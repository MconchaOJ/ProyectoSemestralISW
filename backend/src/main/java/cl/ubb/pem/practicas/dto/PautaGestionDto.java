package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.AmbitoPauta;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.TipoEvaluador;
import java.time.LocalDateTime;

/**
 * HU-06: una pauta (una versión) en la pantalla del coordinador. {@code vigente} = es la última versión (la única que
 * se edita); {@code evaluaciones} = cuántas evaluaciones la usan (si hay alguna, editarla crea una versión nueva y no
 * se puede borrar).
 */
public record PautaGestionDto(Integer id, Integer asignaturaId, String asignatura, String nombre, int version,
                              Integer versionAnteriorId, TipoEvaluador tipoEvaluador, AmbitoPauta ambito, Integer numeroObservacion,
                              Integer escalaMin, Integer escalaMax, boolean activa, boolean vigente,
                              int totalIndicadores, int puntajeMaximo, long evaluaciones, LocalDateTime fechaCreacion) {

    public static PautaGestionDto desde(Pauta p, boolean vigente, long evaluaciones) {
        int indicadores = p.indicadores().size();
        return new PautaGestionDto(p.getId(), p.getAsignatura().getId(), p.getAsignatura().getNombre(), p.getNombre(),
                p.getVersion(), p.getVersionAnterior() == null ? null : p.getVersionAnterior().getId(),
                p.getTipoEvaluador(), p.getAmbito(), p.getNumeroObservacion(), p.getEscalaMin(), p.getEscalaMax(), Boolean.TRUE.equals(p.getActiva()),
                vigente, indicadores, indicadores * p.getEscalaMax(), evaluaciones, p.getFechaCreacion());
    }
}
