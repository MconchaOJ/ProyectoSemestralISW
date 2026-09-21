package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Nueva planificación (queda en BORRADOR). Solo el título es obligatorio: un borrador puede estar incompleto,
 * pero para enviarlo a revisión hay que completar todos los elementos del formato (S-09).
 */
public record PlanificacionCrearRequest(
        @Schema(description = "Inscripción del estudiante (debe ser suya y estar EN_CURSO)", example = "3") @NotNull Long inscripcionId,
        @Schema(example = "Ecuaciones de primer grado en contextos cotidianos") @NotBlank @Size(max = 200) String titulo,
        @Schema(example = "2026-10-07") LocalDate fechaClase,
        @Schema(description = "Objetivo de la clase") @Size(max = 5000) String objetivo,
        @Schema(description = "Recursos que se usarán") @Size(max = 5000) String recursos,
        @Schema(description = "Distribución de los tiempos (inicio, desarrollo, cierre)") @Size(max = 5000) String tiempos,
        @Schema(description = "Duración total de la clase en minutos", example = "90") @Positive Integer duracionMinutos,
        @Schema(description = "Cómo se evaluará el aprendizaje") @Size(max = 5000) String evaluacionAprendizaje,
        @Schema(description = "Cómo interactúan los participantes con los recursos") @Size(max = 5000) String interaccionParticipantes) {
}
