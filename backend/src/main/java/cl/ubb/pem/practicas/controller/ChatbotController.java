package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.ConversacionChatbotDto;
import cl.ubb.pem.practicas.dto.MensajeChatbotRequest;
import cl.ubb.pem.practicas.dto.RespuestaChatbotDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-15: chatbot de retroalimentación de planificaciones (Práctica Pedagógica y Profesional), con el historial del
 * estudiante como contexto. HU-16: el profesor guía de la inscripción lee la conversación. HU-20: el jefe de carrera
 * también la lee. SUPUESTOS S-59 a S-66 en PROGRESO.md (proveedor del modelo, quién lee el chat, cómo se adjunta la
 * planificación).
 */
@RestController
@RequestMapping("/api/inscripciones/{inscripcionId}/chatbot")
@Tag(name = "Chatbot de retroalimentación")
public class ChatbotController {

    private final ChatbotService chatbot;

    public ChatbotController(ChatbotService chatbot) {
        this.chatbot = chatbot;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR_GUIA', 'JEFE_CARRERA')")
    @Operation(summary = "Ver la conversación con el chatbot",
            description = "El estudiante de la inscripción, su profesor guía (HU-16, S-60) y el jefe de carrera en "
                    + "cualquier inscripción de Práctica Pedagógica o Profesional (HU-20, S-66); los demás, 403. La "
                    + "privacidad de los chats sigue abierta (S-38). Indica si el chatbot está disponible y quién "
                    + "puede leer el chat. Funciona también con la práctica cerrada (solo lectura).")
    public ConversacionChatbotDto conversacion(@PathVariable Long inscripcionId,
                                               @AuthenticationPrincipal UsuarioAutenticado actor) {
        return chatbot.conversacion(inscripcionId, actor);
    }

    @PostMapping("/mensajes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Escribir al chatbot y recibir su respuesta",
            description = "Solo el estudiante dueño (403 para otro), con la práctica EN_CURSO y con chatbot "
                    + "habilitado (409 si no). planificacionId (opcional) = una planificación de esta inscripción que "
                    + "el chatbot debe leer, con sus documentos (400 si es de otra). Si el proveedor del modelo no "
                    + "responde o no está configurado: 503 y no se guarda ningún mensaje.")
    public RespuestaChatbotDto enviar(@PathVariable Long inscripcionId, @Valid @RequestBody MensajeChatbotRequest request,
                                      @AuthenticationPrincipal UsuarioAutenticado actor) {
        return chatbot.enviar(inscripcionId, request, actor);
    }
}
