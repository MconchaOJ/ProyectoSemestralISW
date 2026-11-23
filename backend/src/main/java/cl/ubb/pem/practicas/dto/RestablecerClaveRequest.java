package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Restablecer la clave con el enlace del correo. La nueva sigue la regla de HU-26 (8 a 72 caracteres). */
public record RestablecerClaveRequest(
        @Schema(description = "Token que viene en el enlace del correo (parámetro token)")
        @NotBlank @Size(max = 200) String token,
        @Schema(description = "Clave nueva (8 a 72 caracteres)")
        @NotBlank @Size(min = 8, max = 72) String nuevaClave) {
}
