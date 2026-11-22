package cl.ubb.pem.practicas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** HU-09: nota de un componente externo, de 1,0 a 7,0 con un decimal. */
public record NotaExternaRequest(
        @NotNull @DecimalMin("1.0") @DecimalMax("7.0") @Digits(integer = 1, fraction = 1) BigDecimal nota) {
}
