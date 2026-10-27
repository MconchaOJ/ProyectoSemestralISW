package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.ValidacionAbiertaRequest;
import cl.ubb.pem.practicas.dto.ValidacionChatbotDto;
import cl.ubb.pem.practicas.dto.ValidadorRequest;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ValidacionChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-30: validación del chatbot con un grupo de estudiantes (SUPUESTO S-71: la SUS y la rúbrica van fuera). */
@RestController
@RequestMapping("/api/chatbot/validacion")
@Tag(name = "Chatbot de retroalimentación")
@PreAuthorize("hasRole('COORDINADOR')")
public class ValidacionChatbotController {

    private final ValidacionChatbotService validacion;

    public ValidacionChatbotController(ValidacionChatbotService validacion) {
        this.validacion = validacion;
    }

    @GetMapping
    @Operation(summary = "Ver el estado de la validación del chatbot",
            description = "Solo el coordinador. Si está abierta y quiénes son los estudiantes validadores.")
    public ValidacionChatbotDto estado() {
        return validacion.estado();
    }

    @PutMapping
    @Operation(summary = "Abrir o cerrar la validación del chatbot",
            description = "Solo el coordinador. Abierta: el chatbot solo responde a los validadores (los demás "
                    + "estudiantes reciben 409 \"aún no disponible\"). Cerrada: responde a todos.")
    public ValidacionChatbotDto cambiar(@Valid @RequestBody ValidacionAbiertaRequest request,
                                        @AuthenticationPrincipal UsuarioAutenticado actor) {
        return validacion.cambiarApertura(request.abierta(), actor);
    }

    @PutMapping("/validadores/{estudianteId}")
    @Operation(summary = "Marcar o desmarcar a un estudiante como validador",
            description = "Solo el coordinador (404 si el estudiante no existe).")
    public ValidacionChatbotDto marcar(@PathVariable Long estudianteId, @Valid @RequestBody ValidadorRequest request) {
        return validacion.marcarValidador(estudianteId, request.validador());
    }
}
