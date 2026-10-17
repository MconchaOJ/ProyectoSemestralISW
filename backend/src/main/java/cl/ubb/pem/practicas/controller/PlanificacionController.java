package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.PlanificacionActualizarRequest;
import cl.ubb.pem.practicas.dto.PlanificacionCrearRequest;
import cl.ubb.pem.practicas.dto.PlanificacionDto;
import cl.ubb.pem.practicas.dto.PlanificacionResumenDto;
import cl.ubb.pem.practicas.dto.RevisionRequest;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.PlanificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planificaciones")
@Tag(name = "HU-04 Planificación de clase")
public class PlanificacionController {

    private final PlanificacionService planificaciones;

    public PlanificacionController(PlanificacionService planificaciones) {
        this.planificaciones = planificaciones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Crear una planificación (BORRADOR)",
            description = "Solo ESTUDIANTE, en una inscripción suya EN_CURSO. Un borrador puede estar incompleto; "
                    + "solo el título es obligatorio.")
    public PlanificacionDto crear(@Valid @RequestBody PlanificacionCrearRequest request,
                                  @AuthenticationPrincipal UsuarioAutenticado actor) {
        return planificaciones.crear(request, actor);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Editar la planificación",
            description = "Solo su dueño y solo en BORRADOR o CON_OBSERVACIONES. En cualquier otro estado: 409.")
    public PlanificacionDto actualizar(@PathVariable Long id, @Valid @RequestBody PlanificacionActualizarRequest request,
                                       @AuthenticationPrincipal UsuarioAutenticado actor) {
        return planificaciones.actualizar(id, request, actor);
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Enviar (o reenviar) la planificación al profesor guía",
            description = "Solo su dueño. BORRADOR o CON_OBSERVACIONES pasan a EN_REVISION_GUIA. Exige todos los "
                    + "elementos del formato completos (400 si faltan).")
    public PlanificacionDto enviar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return planificaciones.enviar(id, actor);
    }

    @PostMapping("/{id}/revision-guia")
    @PreAuthorize("hasRole('PROFESOR_GUIA')")
    @Operation(summary = "El profesor guía aprueba o devuelve la planificación",
            description = "Solo el guía asignado, con la planificación EN_REVISION_GUIA. APROBADA -> EN_AUTORIZACION. "
                    + "OBSERVADA (comentario obligatorio) -> CON_OBSERVACIONES.")
    public PlanificacionDto revisarComoGuia(@PathVariable Long id, @Valid @RequestBody RevisionRequest request,
                                            @AuthenticationPrincipal UsuarioAutenticado actor) {
        return planificaciones.revisarComoGuia(id, request, actor);
    }

    @PostMapping("/{id}/revision-curso")
    @PreAuthorize("hasRole('PROFESOR_CURSO')")
    @Operation(summary = "El profesor del curso autoriza o devuelve la planificación",
            description = "Solo el profesor del curso asignado, con la planificación EN_AUTORIZACION. APROBADA -> AUTORIZADA. "
                    + "OBSERVADA (comentario obligatorio) -> CON_OBSERVACIONES y el flujo se repite desde el guía.")
    public PlanificacionDto revisarComoProfesorDelCurso(@PathVariable Long id, @Valid @RequestBody RevisionRequest request,
                                                        @AuthenticationPrincipal UsuarioAutenticado actor) {
        return planificaciones.revisarComoProfesorDelCurso(id, request, actor);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una planificación con su historial de revisiones",
            description = "El estudiante dueño, su profesor guía, su profesor del curso, el coordinador y el jefe de carrera, en cualquier estado. "
                    + "Otro usuario: 403. Las revisiones vienen en orden cronológico.")
    public PlanificacionDto obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return planificaciones.obtener(id, actor);
    }

    @GetMapping
    @Operation(summary = "Listar planificaciones (según mi rol)",
            description = "Bandeja de cada rol: el estudiante ve las suyas; el guía y el profesor del curso, las de sus "
                    + "inscripciones asignadas; el coordinador y el jefe de carrera, todas. Filtros opcionales por inscripción y estado "
                    + "(por ejemplo estado=EN_REVISION_GUIA para la bandeja pendiente del guía).")
    public List<PlanificacionResumenDto> listar(@RequestParam(required = false) Long inscripcionId,
                                                @RequestParam(required = false) EstadoPlanificacion estado,
                                                @AuthenticationPrincipal UsuarioAutenticado actor) {
        return planificaciones.listar(inscripcionId, estado, actor);
    }
}
