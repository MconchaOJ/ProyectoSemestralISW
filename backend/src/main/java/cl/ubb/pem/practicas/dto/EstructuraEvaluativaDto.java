package cl.ubb.pem.practicas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estructura evaluativa de una asignatura (HU-09): sus componentes con su peso. {@code completa} es verdadero si
 * hay al menos un componente y los pesos suman 100; solo entonces se calcula la nota final.
 */
public record EstructuraEvaluativaDto(Integer asignaturaId, String asignatura, List<ComponenteNotaDto> componentes,
                                      BigDecimal sumaPesos, boolean completa) {
}
