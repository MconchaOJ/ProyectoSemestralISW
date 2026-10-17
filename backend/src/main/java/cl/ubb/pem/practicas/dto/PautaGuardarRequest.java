package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.AmbitoPauta;
import cl.ubb.pem.practicas.entity.TipoEvaluador;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * HU-06: la pauta completa que el coordinador crea o edita. Se envía siempre entera (no por partes): dimensiones en
 * orden y, dentro de cada una, los textos de sus indicadores en orden. El número de cada indicador lo asigna el
 * sistema (1, 2, 3... a lo largo de toda la pauta, como en la pauta impresa).
 */
public record PautaGuardarRequest(
        @Schema(description = "Asignatura de práctica (1 a 6). No se cambia al editar", example = "3")
        @NotNull Integer asignaturaId,
        @Schema(example = "Pauta de Especialidad - Tutor (Práctica Profesional 2027)")
        @NotBlank @Size(max = 200) String nombre,
        @Schema(description = "Quién la aplica: COLABORADOR (profesor guía), TUTOR o ACADEMICO (profesor del curso)")
        @NotNull TipoEvaluador tipoEvaluador,
        @Schema(description = "OBSERVACION_CLASE, ESPECIALIDAD u ORIENTACION")
        @NotNull AmbitoPauta ambito,
        @Schema(description = "HU-07: 1 o 2 (primera o segunda observación). Obligatorio en ESPECIALIDAD y ORIENTACION; "
                + "vacío en OBSERVACION_CLASE", example = "1")
        @Min(1) @Max(2) Integer numeroObservacion,
        @Schema(example = "1") @NotNull @Min(0) @Max(100) Integer escalaMin,
        @Schema(example = "7") @NotNull @Min(1) @Max(100) Integer escalaMax,
        @NotEmpty @Size(max = 20) @Valid List<Dimension> dimensiones) {

    public record Dimension(
            @Schema(example = "Aspectos de la estructura de la clase") @NotBlank @Size(max = 150) String nombre,
            @Schema(description = "Textos de los indicadores, en orden")
            @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 2000) String> indicadores) {
    }
}
