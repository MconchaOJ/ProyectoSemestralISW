package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mensaje del estudiante al chatbot (HU-15). */
public record MensajeChatbotRequest(
        @Schema(example = "¿Qué te parece el cierre de mi clase? Quiero saber si verifica el objetivo.")
        @NotBlank @Size(max = 4000) String texto,
        @Schema(description = "Planificación propia (de esta inscripción) que el chatbot debe leer: su formulario y sus "
                + "documentos adjuntos. Opcional: si no se envía, se usa la última que el estudiante adjuntó en el chat",
                example = "12")
        Long planificacionId) {
}
