package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * HU-09: la estructura evaluativa completa de una asignatura, en orden. Reemplaza la anterior: los componentes con
 * {@code id} se actualizan, los sin {@code id} se crean y los que no vienen se borran (409 si ya tienen notas). Si
 * hay componentes, los pesos deben sumar 100.
 */
public record EstructuraEvaluativaRequest(@NotNull @Size(max = 15) List<@NotNull @Valid Componente> componentes) {

    public record Componente(
            @Schema(description = "Id del componente que se edita; vacío para uno nuevo") Integer id,
            @Schema(example = "Integración curricular: TANEP") @NotBlank @Size(max = 150) String nombre,
            @Schema(description = "Porcentaje de la nota final", example = "20")
            @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") @Digits(integer = 3, fraction = 2)
            BigDecimal peso) {
    }
}
