package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.TipoComponente;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-09: la nota de una inscripción calculada con la estructura evaluativa de su asignatura.
 * {@code notaParcial}: con lo que ya tiene nota, recalculada sobre {@code porcentajeEvaluado} (nula si nada tiene nota).
 * {@code notaPropuesta}: solo cuando todos los componentes tienen nota (el 100 %); es la que se propone al cerrar.
 * {@code notaFinalGuardada}: la que confirmó el coordinador al cerrar (inscripcion.nota_final).
 */
public record NotaFinalDto(Long inscripcionId, EstudianteResumenDto estudiante, Integer asignaturaId, String asignatura,
                           String periodo, EstadoInscripcion estado, BigDecimal notaFinalGuardada,
                           boolean tieneEstructura, List<ComponenteNota> componentes, int porcentajeEvaluado,
                           BigDecimal notaParcial, boolean completa, BigDecimal notaPropuesta, boolean puedeEditarNotas) {

    /**
     * {@code nota}: la del componente (nula si aún no tiene). En los de PAUTA, {@code evaluaciones} = cuántas
     * evaluaciones finalizadas se promediaron; en los EXTERNO, quién la escribió y cuándo.
     */
    public record ComponenteNota(Integer componenteId, TipoComponente tipo, String nombre, int porcentaje, BigDecimal nota,
                                 int evaluaciones, UsuarioResumenDto registradaPor, LocalDateTime fechaRegistro) {
    }
}
