package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.HistorialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estudiantes")
@Tag(name = "HU-05 Historial del estudiante")
public class HistorialController {

    private final HistorialService historial;

    public HistorialController(HistorialService historial) {
        this.historial = historial;
    }

    @GetMapping("/me/historial")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Mi historial",
            description = "Solo ESTUDIANTE: su propio recorrido, sin necesidad de conocer su id.")
    public HistorialDto miHistorial(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return historial.historialPropio(actor);
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Historial (recorrido) de un estudiante",
            description = "Inscripciones en orden cronológico con asignatura, ciclo, periodo, estado y nota; cada una con sus "
                    + "evaluaciones finalizadas (fecha, pauta, puntaje total, promedio) y sus planificaciones con su estado. "
                    + "Incluye el semestre actual y las asignaturas aprobadas. El detalle indicador por indicador de una "
                    + "evaluación está en GET /api/evaluaciones/{id}. Acceso: el propio estudiante, su guía o profesor del "
                    + "curso asignados, y el coordinador; cualquier otro recibe 403.")
    public HistorialDto historialDe(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return historial.historialDe(id, actor);
    }
}
