package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Puntajes de uno o varios indicadores. Si un indicador ya tenía puntaje, se reemplaza. */
public record RespuestasRequest(@NotEmpty @Valid List<Item> respuestas) {

    public record Item(
            @Schema(description = "Id del indicador (campo `id` de GET /api/pautas, no el número impreso)", example = "1")
            @NotNull Integer indicadorId,
            @Schema(description = "Entero dentro de la escala de la pauta (1 a 7)", example = "5") @NotNull Integer puntaje,
            @Size(max = 500) String comentario) {
    }
}
