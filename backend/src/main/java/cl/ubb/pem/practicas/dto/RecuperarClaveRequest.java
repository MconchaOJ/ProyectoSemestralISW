package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedir el enlace para restablecer la clave (S-97 a S-99). */
public record RecuperarClaveRequest(
        @Schema(description = "Correo de la cuenta", example = "coordinador@demo.example")
        @NotBlank @Size(max = 150) String email) {
}
