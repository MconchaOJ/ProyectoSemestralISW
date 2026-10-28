package cl.ubb.pem.practicas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Chatbot de retroalimentación (HU-15, prefijo app.chatbot). Proveedor del modelo: Groq, con su API compatible con
 * "chat completions" de OpenAI (SUPUESTO S-59: el cliente no ha confirmado el proveedor).
 *
 * @param apiKey          clave del proveedor; SOLO desde la variable de entorno ISW_GROQ_API_KEY. Vacía = el chatbot
 *                        no está configurado: la aplicación arranca igual y enviar un mensaje responde 503
 * @param modelo          id del modelo en Groq (la elección y el motivo están en PROGRESO.md)
 * @param maxTokens       tope de la respuesta (max_completion_tokens)
 * @param temperatura     0 a 2; baja para que siga las reglas con más constancia
 * @param timeoutSegundos cuánto se espera al proveedor antes de rendirse (el estudiante está esperando)
 * @param reintentos      reintentos ante una falla de red o un error 5xx del proveedor (nunca ante un 429: es el
 *                        límite de uso y reintentar solo lo agota más)
 * @param urlBase         dirección de la API (las pruebas la apuntan a un servidor local)
 */
@ConfigurationProperties(prefix = "app.chatbot")
public record ChatbotProperties(@DefaultValue("") String apiKey,
                                @DefaultValue("openai/gpt-oss-120b") String modelo,
                                @DefaultValue("3000") int maxTokens,
                                @DefaultValue("0.3") double temperatura,
                                @DefaultValue("60") int timeoutSegundos,
                                @DefaultValue("2") int reintentos,
                                @DefaultValue("https://api.groq.com/openai/v1") String urlBase) {

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }
}
