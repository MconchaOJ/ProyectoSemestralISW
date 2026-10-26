package cl.ubb.pem.practicas.dto;

import java.util.List;

/**
 * La conversación de una inscripción con el chatbot (HU-15 y HU-16).
 *
 * @param disponible         el chatbot se puede usar en esta inscripción (práctica final y EN_CURSO)
 * @param motivoNoDisponible por qué no, en palabras para el usuario (nulo si está disponible)
 * @param puedeEscribir      quien consulta puede enviar mensajes: solo el estudiante dueño y con el chatbot disponible
 * @param profesorGuia       quién más puede leer el chat (HU-16, criterio 2: el estudiante sabe que su guía lo ve)
 */
public record ConversacionChatbotDto(Long inscripcionId, String estudiante, String asignatura, String periodo,
                                     boolean disponible, String motivoNoDisponible, boolean puedeEscribir,
                                     UsuarioResumenDto profesorGuia, List<MensajeChatbotDto> mensajes) {
}
