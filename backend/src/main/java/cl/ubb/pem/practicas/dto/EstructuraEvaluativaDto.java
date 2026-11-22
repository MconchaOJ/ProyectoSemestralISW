package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.TipoComponente;
import java.util.List;

/**
 * HU-09: la estructura evaluativa de una asignatura. {@code puedeEditar} = quien consulta la puede cambiar (el
 * coordinador o un profesor del curso de esa asignatura). {@code pautasDisponibles}: las versiones vigentes de las
 * pautas de la asignatura, para elegir la de un componente de tipo PAUTA.
 */
public record EstructuraEvaluativaDto(Integer asignaturaId, String asignatura, List<ComponenteDto> componentes,
                                      int porcentajeTotal, int porcentajePautas, int porcentajeExternos,
                                      boolean puedeEditar, List<PautaOpcion> pautasDisponibles) {

    /** {@code notasRegistradas}: notas externas ya escritas (se borran si se quita el componente). */
    public record ComponenteDto(Integer id, TipoComponente tipo, String nombre, Integer pautaId, String pautaNombre,
                                Integer pautaVersion, int porcentaje, int orden, long notasRegistradas) {
    }

    public record PautaOpcion(Integer id, String nombre, Integer version, boolean activa) {
    }
}
