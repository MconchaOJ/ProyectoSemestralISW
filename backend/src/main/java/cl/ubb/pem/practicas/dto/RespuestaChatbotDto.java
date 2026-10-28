package cl.ubb.pem.practicas.dto;

/** Lo que devuelve enviar un mensaje (HU-15): el mensaje del estudiante y la respuesta del chatbot, ya guardados. */
public record RespuestaChatbotDto(MensajeChatbotDto mensajeEstudiante, MensajeChatbotDto respuesta) {
}
