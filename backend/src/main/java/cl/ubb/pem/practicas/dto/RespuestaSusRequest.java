package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** La Escala SUS de un validador (HU-30): los 10 ítems en orden, cada uno de 1 a 5. */
public record RespuestaSusRequest(
        @Schema(example = "[4, 2, 5, 1, 4, 2, 4, 2, 4, 2]")
        @NotNull @Size(min = 10, max = 10) List<@NotNull @Min(1) @Max(5) Integer> items) {
}
