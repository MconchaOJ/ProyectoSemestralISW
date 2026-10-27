package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Estado de la validación del chatbot (HU-30). */
public record ValidacionChatbotDto(
        @Schema(description = "true = el chatbot solo responde a los estudiantes validadores") boolean abierta,
        List<EstudianteResumenDto> validadores) {
}
