package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Texto nuevo de las instrucciones del chatbot (HU-17). El largo (200 a 12 000 caracteres, SUPUESTO S-67) lo
 * comprueba el servicio, que es también quien lo informa a la pantalla.
 */
public record InstruccionesChatbotRequest(
        @Schema(description = "Instrucciones completas: reglas, tono, expresiones a usar y evitar, respuestas modelo")
        @NotBlank String texto) {
}
