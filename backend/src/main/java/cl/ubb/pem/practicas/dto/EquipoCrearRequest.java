package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** HU-10: armar una dupla o un grupo con inscripciones de la misma asignatura y periodo. */
public record EquipoCrearRequest(
        @Schema(description = "Inscripciones de los integrantes: 2 en una dupla, de 2 a 4 en un grupo",
                example = "[25, 27]")
        @NotNull @Size(min = 2, max = 4) List<@NotNull Long> inscripcionIds,
        @Schema(description = "Colegio compartido (obligatorio si la asignatura va a un colegio)",
                example = "Liceo Bicentenario Ejemplo")
        @Size(max = 150) String establecimiento,
        @Schema(description = "Curso compartido (obligatorio si la asignatura va a un colegio)", example = "1° Medio B")
        @Size(max = 50) String curso) {
}
