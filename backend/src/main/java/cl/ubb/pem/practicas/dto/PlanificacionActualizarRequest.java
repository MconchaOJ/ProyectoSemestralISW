package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Reemplaza los campos (formato oficial, S-45) de una planificación en BORRADOR o CON_OBSERVACIONES. */
public record PlanificacionActualizarRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 150) String establecimiento,
        @Size(max = 50) String curso,
        @Size(max = 200) String unidad,
        @Size(max = 5000) String objetivoAprendizaje,
        @Size(max = 5000) String objetivoActitudinal,
        @Size(max = 5000) String objetivoClase,
        @Schema(example = "2026-10-07") LocalDate fechaClase,
        @Schema(example = "90") @Positive Integer duracionMinutos,
        @Size(max = 5000) String recursos,
        @Size(max = 5000) String instrumentosEvaluacion,
        @Size(max = 5000) String inicio,
        @Schema(example = "10") @Positive Integer inicioMinutos,
        @Size(max = 5000) String desarrollo,
        @Schema(example = "60") @Positive Integer desarrolloMinutos,
        @Size(max = 5000) String cierre,
        @Schema(example = "20") @Positive Integer cierreMinutos) implements CamposPlanificacion {
}
