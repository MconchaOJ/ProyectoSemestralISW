package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** HU-06: activar o desactivar la versión vigente de una pauta (una desactivada no se ofrece para evaluar). */
public record PautaActivaRequest(@Schema(example = "false") @NotNull Boolean activa) {
}
