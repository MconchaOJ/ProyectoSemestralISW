package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EstudianteCrearRequest(
        @Schema(example = "Ana") @NotBlank @Size(max = 80) String nombre,
        @Schema(example = "Soto Reyes") @NotBlank @Size(max = 120) String apellidos,
        @Schema(example = "ana.soto@demo.example") @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "no es un correo electrónico válido") String email,
        @Schema(description = "Contraseña inicial que define el coordinador (8 a 72 caracteres)")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(description = "Con o sin puntos; se valida el dígito verificador", example = "12345678-5")
        @NotBlank @Size(max = 12) String rut,
        @Schema(example = "6") @NotNull @Min(1) @Max(12) Integer semestreActual,
        @Schema(example = "2024") @NotNull @Min(2000) @Max(2100) Integer anioIngreso) {
}
