package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * HU-10: crear una dupla o grupo, o reemplazar sus integrantes. Las inscripciones deben ser de la misma asignatura y
 * periodo, estar EN_CURSO, no estar en otro grupo y tener el mismo profesor guía; una dupla lleva 2 y un grupo de 2 a
 * 4 (S-78). El establecimiento y el curso solo se aceptan en las prácticas que van a un colegio.
 */
public record GrupoRequest(
        @Schema(description = "Inscripciones que forman el grupo", example = "[29, 31]")
        @NotNull @Size(min = 2, max = 4, message = "debe tener entre 2 y 4 inscripciones") List<Long> inscripcionIds,
        @Schema(description = "Colegio que comparten (solo prácticas en un colegio)", example = "Liceo Bicentenario Ejemplo")
        @Size(max = 150) String establecimiento,
        @Schema(description = "Curso del colegio que comparten", example = "1° Medio B") @Size(max = 50) String curso) {
}
