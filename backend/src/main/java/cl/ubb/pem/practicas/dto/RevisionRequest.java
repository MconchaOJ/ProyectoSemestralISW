package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.DecisionRevision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RevisionRequest(
        @Schema(description = "APROBADA (avanza en el flujo) u OBSERVADA (vuelve al estudiante)", example = "APROBADA")
        @NotNull DecisionRevision decision,
        @Schema(description = "Obligatorio si la decisión es OBSERVADA", example = "Precisar la evaluación del cierre.")
        @Size(max = 5000) String comentario) {
}
