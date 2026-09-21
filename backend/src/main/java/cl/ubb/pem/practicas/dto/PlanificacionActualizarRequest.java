package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Reemplaza los elementos de una planificación en BORRADOR o CON_OBSERVACIONES. */
public record PlanificacionActualizarRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Schema(example = "2026-10-07") LocalDate fechaClase,
        @Size(max = 5000) String objetivo,
        @Size(max = 5000) String recursos,
        @Size(max = 5000) String tiempos,
        @Schema(example = "90") @Positive Integer duracionMinutos,
        @Size(max = 5000) String evaluacionAprendizaje,
        @Size(max = 5000) String interaccionParticipantes) {
}
