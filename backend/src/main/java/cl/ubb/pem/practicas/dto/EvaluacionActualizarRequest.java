package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** Cabecera y observaciones generales de una evaluación en BORRADOR (reemplaza los valores actuales). */
public record EvaluacionActualizarRequest(
        @Schema(description = "Opcional: planificación AUTORIZADA de la clase observada") Long planificacionId,
        @NotNull LocalDate fechaClase,
        @NotBlank @Size(max = 150) String establecimiento,
        @NotBlank @Size(max = 50) String curso,
        @Schema(type = "string", example = "10:15:00") @NotNull LocalTime horaInicio,
        @Schema(type = "string", example = "11:45:00") @NotNull LocalTime horaTermino,
        @NotBlank @Size(max = 5000) String objetivoClase,
        @Schema(description = "Fortalezas y aspectos a mejorar") @Size(max = 5000) String observacionesGenerales) {
}
