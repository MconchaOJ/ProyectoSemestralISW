package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.InstruccionesChatbotDto;
import cl.ubb.pem.practicas.dto.InstruccionesChatbotRequest;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.CalibracionChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-17: el coordinador calibra el estilo de retroalimentación del chatbot (SUPUESTOS S-65, S-67 y S-68). */
@RestController
@RequestMapping("/api/chatbot/instrucciones")
@Tag(name = "Chatbot de retroalimentación")
@PreAuthorize("hasRole('COORDINADOR')")
public class CalibracionChatbotController {

    private final CalibracionChatbotService calibracion;

    public CalibracionChatbotController(CalibracionChatbotService calibracion) {
        this.calibracion = calibracion;
    }

    @GetMapping
    @Operation(summary = "Ver las instrucciones del chatbot",
            description = "Solo el coordinador (403 para los demás roles). Devuelve el texto vigente, quién y cuándo lo "
                    + "editó, y el texto base de RetroPlani Mate para volver a él.")
    public InstruccionesChatbotDto obtener() {
        return calibracion.obtener();
    }

    @PutMapping
    @Operation(summary = "Calibrar las instrucciones del chatbot",
            description = "Solo el coordinador. Reemplaza el texto completo (200 a 12 000 caracteres, 400 si no). Rige "
                    + "desde el siguiente mensaje de cualquier conversación; los mensajes ya guardados no cambian. El "
                    + "contexto de cada estudiante (semestre, prácticas aprobadas) lo sigue agregando el sistema.")
    public InstruccionesChatbotDto actualizar(@Valid @RequestBody InstruccionesChatbotRequest request,
                                              @AuthenticationPrincipal UsuarioAutenticado actor) {
        return calibracion.actualizar(request, actor);
    }
}
