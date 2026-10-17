package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-26: cambio de la propia contraseña. La nueva sigue la misma regla que al crear un usuario (8 a 72 caracteres). */
public record CambioPasswordRequest(
        @Schema(description = "Contraseña actual del usuario") @NotBlank String passwordActual,
        @Schema(description = "Contraseña nueva (8 a 72 caracteres), distinta de la actual")
        @NotBlank @Size(min = 8, max = 72) String passwordNueva) {
}
