package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record EvaluacionCrearRequest(
        @Schema(description = "Inscripción del estudiante evaluado (debe estar asignada al guía que evalúa)", example = "3")
        @NotNull Long inscripcionId,
        @Schema(description = "Pauta a aplicar (la de observación de clase tiene id 1)", example = "1") @NotNull Integer pautaId,
        @Schema(description = "Opcional: planificación AUTORIZADA de la clase observada") Long planificacionId,
        @Schema(example = "2026-09-22") @NotNull LocalDate fechaClase,
        @Schema(example = "Liceo Bicentenario Ejemplo") @NotBlank @Size(max = 150) String establecimiento,
        @Schema(example = "1° Medio B") @NotBlank @Size(max = 50) String curso,
        @Schema(type = "string", example = "10:15:00") @NotNull LocalTime horaInicio,
        @Schema(type = "string", example = "11:45:00") @NotNull LocalTime horaTermino,
        @Schema(example = "Resolver ecuaciones lineales con una incógnita.") @NotBlank @Size(max = 5000) String objetivoClase,
        @Schema(description = "Fortalezas y aspectos a mejorar") @Size(max = 5000) String observacionesGenerales) {
}
