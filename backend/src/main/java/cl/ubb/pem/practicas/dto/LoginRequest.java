package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "coordinador@demo.example") @NotBlank String email,
        @Schema(description = "Contraseña del usuario", example = "su-contraseña") @NotBlank String password) {
}
