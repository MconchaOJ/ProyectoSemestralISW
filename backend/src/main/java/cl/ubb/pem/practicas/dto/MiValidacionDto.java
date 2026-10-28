package cl.ubb.pem.practicas.dto;

import java.time.LocalDateTime;

/**
 * Lo que ve un estudiante sobre la validación del chatbot (HU-30).
 *
 * @param validador         el coordinador lo marcó como validador (solo entonces puede responder)
 * @param susRespondida     cuándo respondió la Escala SUS (nulo si no la ha respondido)
 * @param rubricaRespondida cuándo respondió la rúbrica (nulo si no la ha respondido)
 */
public record MiValidacionDto(boolean validador, LocalDateTime susRespondida, LocalDateTime rubricaRespondida,
                              InstrumentosValidacionDto instrumentos) {
}
