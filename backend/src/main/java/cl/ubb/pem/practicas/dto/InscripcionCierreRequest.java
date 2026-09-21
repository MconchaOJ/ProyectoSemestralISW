package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InscripcionCierreRequest(
        @Schema(description = "APROBADA o REPROBADA", example = "APROBADA") @NotNull EstadoInscripcion estado,
        @Schema(example = "5.8") @NotNull @DecimalMin("1.0") @DecimalMax("7.0") @Digits(integer = 1, fraction = 1)
        BigDecimal notaFinal) {
}
