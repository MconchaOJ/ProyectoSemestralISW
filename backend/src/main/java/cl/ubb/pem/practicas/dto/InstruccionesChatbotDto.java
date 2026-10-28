package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Las instrucciones vigentes del chatbot, para que el coordinador las calibre (HU-17). */
public record InstruccionesChatbotDto(
        @Schema(description = "Texto vigente: reglas, tono, expresiones y respuestas modelo") String texto,
        @Schema(description = "true si el texto vigente es igual al texto base de RetroPlani Mate") boolean esTextoBase,
        @Schema(description = "Quién lo editó por última vez; nulo si nunca se editó") UsuarioResumenDto editadoPor,
        LocalDateTime fechaEdicion,
        @Schema(description = "Texto base de RetroPlani Mate, para volver a él") String textoBase,
        int minimoCaracteres,
        int maximoCaracteres) {
}
