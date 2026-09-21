package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InscripcionCrearRequest(
        @Schema(description = "Id del estudiante (no del usuario)", example = "5") @NotNull Long estudianteId,
        @Schema(description = "Id de la asignatura de práctica (1 a 6)", example = "2") @NotNull Integer asignaturaId,
        @Schema(description = "Año y semestre", example = "2026-2")
        @NotNull @Pattern(regexp = "^[0-9]{4}-[12]$", message = "debe tener el formato AAAA-S, por ejemplo 2026-2") String periodo,
        @Schema(description = "Id del profesor guía. Obligatorio si la asignatura se hace en un centro educativo; "
                + "puede ir vacío en el ciclo inicial", example = "2") Long profesorGuiaId,
        @Schema(description = "Id del profesor del curso, que autoriza las clases", example = "4")
        @NotNull Long profesorCursoId) {
}
