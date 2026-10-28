package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La nota final ponderada de una inscripción (HU-09): cada componente de la asignatura con su nota (si ya está) y su
 * aporte (nota × peso / 100), la nota calculada (nula mientras falte alguno o la estructura no sume 100) y la nota
 * de cierre que puso el coordinador. {@code puedeRegistrar} dice si quien consulta puede poner notas aquí.
 */
public record NotaFinalDto(Long inscripcionId, EstadoInscripcion estado, List<LineaDto> componentes,
                           BigDecimal sumaPesos, boolean estructuraCompleta, int registradas,
                           BigDecimal notaCalculada, BigDecimal notaFinal, boolean puedeRegistrar) {

    public record LineaDto(Integer componenteId, String nombre, BigDecimal peso, BigDecimal nota, BigDecimal aporte,
                           String registradaPor, LocalDateTime fechaRegistro) {
    }
}
