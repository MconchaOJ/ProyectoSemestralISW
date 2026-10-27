package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.MiValidacionDto;
import cl.ubb.pem.practicas.dto.RespuestaRubricaRequest;
import cl.ubb.pem.practicas.dto.RespuestaSusRequest;
import cl.ubb.pem.practicas.dto.ResultadosValidacionDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.RespuestasValidacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HU-30: la Escala SUS y la rúbrica de juicio experto del chatbot, dentro del sistema (SUPUESTO S-75). */
@RestController
@RequestMapping("/api/chatbot/validacion")
@Tag(name = "Chatbot de retroalimentación")
public class RespuestasValidacionController {

    private final RespuestasValidacionService respuestas;

    public RespuestasValidacionController(RespuestasValidacionService respuestas) {
        this.respuestas = respuestas;
    }

    @GetMapping("/mi-validacion")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Ver si soy validador y qué instrumentos ya respondí",
            description = "Solo estudiantes. Incluye los textos de la Escala SUS y de la rúbrica.")
    public MiValidacionDto miValidacion(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return respuestas.miEstado(actor);
    }

    @PostMapping("/sus")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Responder la Escala SUS del chatbot",
            description = "Solo un estudiante validador (403 si no lo es), una sola vez (409). 10 ítems de 1 a 5.")
    public MiValidacionDto responderSus(@Valid @RequestBody RespuestaSusRequest request,
                                        @AuthenticationPrincipal UsuarioAutenticado actor) {
        return respuestas.responderSus(request, actor);
    }

    @PostMapping("/rubrica")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Responder la rúbrica de juicio experto del chatbot",
            description = "Solo un estudiante validador (403 si no lo es), una sola vez (409). 9 criterios de 1 a 4 "
                    + "con comentario opcional y 4 preguntas abiertas opcionales.")
    public MiValidacionDto responderRubrica(@Valid @RequestBody RespuestaRubricaRequest request,
                                            @AuthenticationPrincipal UsuarioAutenticado actor) {
        return respuestas.responderRubrica(request, actor);
    }

    @GetMapping("/resultados")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Ver los resultados de la validación del chatbot",
            description = "Solo el coordinador. Promedios de la Escala SUS (puntaje 0 a 100 y por ítem) y de la "
                    + "rúbrica (por criterio), y cada respuesta con quién y cuándo respondió.")
    public ResultadosValidacionDto resultados() {
        return respuestas.resultados();
    }
}
