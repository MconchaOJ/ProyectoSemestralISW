package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfesorCrearRequest(
        @Schema(example = "Camilo") @NotBlank @Size(max = 80) String nombre,
        @Schema(example = "Araya Lagos") @NotBlank @Size(max = 120) String apellidos,
        @Schema(example = "camilo.araya@demo.example") @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "no es un correo electrónico válido") String email,
        @Schema(description = "Contraseña inicial que define el coordinador (8 a 72 caracteres)")
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull RolProfesor rol) {
}
