package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Nueva planificación (queda en BORRADOR), con los campos del formato oficial de la carrera (S-45). Solo el título
 * es obligatorio: un borrador puede estar incompleto, pero para enviarlo a revisión hay que completar todos los
 * campos. Si vienen los cuatro, inicio + desarrollo + cierre debe sumar la duración (S-76, 400 si no).
 */
public record PlanificacionCrearRequest(
        @Schema(description = "Inscripción del estudiante (debe ser suya y estar EN_CURSO)", example = "3") @NotNull Long inscripcionId,
        @Schema(example = "Ecuaciones de primer grado en contextos cotidianos") @NotBlank @Size(max = 200) String titulo,
        @Schema(example = "Liceo Bicentenario Ejemplo") @Size(max = 150) String establecimiento,
        @Schema(example = "1° Medio B") @Size(max = 50) String curso,
        @Schema(example = "Álgebra y funciones") @Size(max = 200) String unidad,
        @Schema(description = "Objetivo de aprendizaje / indicadores de evaluación") @Size(max = 5000) String objetivoAprendizaje,
        @Schema(description = "Objetivo de aprendizaje actitudinal") @Size(max = 5000) String objetivoActitudinal,
        @Schema(description = "Objetivo de la clase") @Size(max = 5000) String objetivoClase,
        @Schema(example = "2026-10-07") LocalDate fechaClase,
        @Schema(description = "Duración total de la clase en minutos", example = "90") @Positive Integer duracionMinutos,
        @Schema(description = "Recursos que se usarán") @Size(max = 5000) String recursos,
        @Schema(description = "Instrumentos de evaluación") @Size(max = 5000) String instrumentosEvaluacion,
        @Schema(description = "Inicio: relato de lo que se realizará") @Size(max = 5000) String inicio,
        @Schema(example = "10") @Positive Integer inicioMinutos,
        @Schema(description = "Desarrollo: relato de lo que se realizará") @Size(max = 5000) String desarrollo,
        @Schema(example = "60") @Positive Integer desarrolloMinutos,
        @Schema(description = "Cierre: relato de lo que se realizará") @Size(max = 5000) String cierre,
        @Schema(example = "20") @Positive Integer cierreMinutos) implements CamposPlanificacion {
}
