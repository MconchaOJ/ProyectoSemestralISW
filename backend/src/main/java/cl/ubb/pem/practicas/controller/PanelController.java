package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.PanelDto;
import cl.ubb.pem.practicas.service.PanelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/panel")
@Tag(name = "HU-14 Panel de monitoreo del coordinador")
public class PanelController {

    private final PanelService panel;

    public PanelController(PanelService panel) {
        this.panel = panel;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINADOR','JEFE_CARRERA')")
    @Operation(summary = "Panel de monitoreo",
            description = "Solo COORDINADOR y JEFE_CARRERA. Cifras del filtro, una fila por asignatura, las "
                    + "planificaciones pendientes (esperando al guía, al profesor del curso o, si fueron devueltas, al "
                    + "estudiante) con los días que llevan esperando, los estudiantes EN_CURSO sin ninguna evaluación "
                    + "finalizada (solo en asignaturas con pauta, S-81) y el avance de cada inscripción. Filtros "
                    + "opcionales; 400 si el periodo no es AAAA-S.")
    public PanelDto panel(
            @Parameter(description = "Periodo AAAA-S; vacío = todos", example = "2026-2")
            @RequestParam(required = false) String periodo,
            @Parameter(description = "Id de la asignatura; vacío = todas", example = "3")
            @RequestParam(required = false) Integer asignaturaId) {
        return panel.panel(periodo, asignaturaId);
    }
}
