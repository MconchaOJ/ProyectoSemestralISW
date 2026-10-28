package cl.ubb.pem.practicas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * La rúbrica de juicio experto de un validador (HU-30): los 9 criterios en orden (puntaje de 1 a 4 y comentario
 * opcional) y las 4 preguntas abiertas, también opcionales.
 */
public record RespuestaRubricaRequest(
        @NotNull @Size(min = 9, max = 9) List<@NotNull @Valid Criterio> criterios,
        @Size(max = 4000) String fortalezas,
        @Size(max = 4000) String debilidades,
        @Size(max = 4000) String mejoras,
        @Size(max = 4000) String recomendacion) {

    public record Criterio(@NotNull @Min(1) @Max(4) Integer puntaje, @Size(max = 1000) String comentario) {
    }
}
