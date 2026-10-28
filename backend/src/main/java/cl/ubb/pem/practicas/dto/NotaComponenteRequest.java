package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** HU-09: la nota de un componente, en la escala 1,0 a 7,0 con un decimal (SUPUESTO S-78). */
public record NotaComponenteRequest(
        @Schema(example = "6.2") @NotNull @DecimalMin("1.0") @DecimalMax("7.0") @Digits(integer = 1, fraction = 1)
        BigDecimal nota) {
}
