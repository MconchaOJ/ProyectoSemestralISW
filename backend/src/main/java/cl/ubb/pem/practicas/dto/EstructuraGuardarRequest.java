package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.TipoComponente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * HU-09: la estructura completa de una asignatura, en orden. Un componente con {@code id} se actualiza; sin {@code id}
 * se crea; los que ya no vienen se quitan (con sus notas externas). Lista vacía = la asignatura queda sin estructura.
 */
public record EstructuraGuardarRequest(@NotNull @Valid List<Componente> componentes) {

    public record Componente(
            @Schema(description = "Id de un componente existente; nulo para uno nuevo") Integer id,
            @NotNull TipoComponente tipo,
            @Schema(description = "Obligatorio en los EXTERNO; en los de PAUTA, si va vacío se usa el nombre de la pauta")
            @Size(max = 150) String nombre,
            @Schema(description = "Solo en los de tipo PAUTA: la versión vigente de una pauta de la asignatura") Integer pautaId,
            @NotNull @Min(1) @Max(100) Integer porcentaje) {
    }
}
