package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.EvaluacionActualizarRequest;
import cl.ubb.pem.practicas.dto.EvaluacionCrearRequest;
import cl.ubb.pem.practicas.dto.EvaluacionDto;
import cl.ubb.pem.practicas.dto.EvaluacionResumenDto;
import cl.ubb.pem.practicas.dto.RespuestasRequest;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.EvaluacionService;
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
@RequestMapping("/api/evaluaciones")
@Tag(name = "HU-03 Pauta digital y evaluaciones")
public class EvaluacionController {

    private final EvaluacionService evaluaciones;

    public EvaluacionController(EvaluacionService evaluaciones) {
        this.evaluaciones = evaluaciones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESOR_GUIA')")
    @Operation(summary = "Crear una evaluación (queda en BORRADOR)",
            description = "Solo PROFESOR_GUIA, y solo sobre inscripciones que tiene asignadas (otro guía: 403). "
                    + "Datos de la cabecera de la pauta en papel: fecha, establecimiento, curso, horas y objetivo.")
    public EvaluacionDto crear(@Valid @RequestBody EvaluacionCrearRequest request,
                               @AuthenticationPrincipal UsuarioAutenticado actor) {
        return evaluaciones.crear(request, actor);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROFESOR_GUIA')")
    @Operation(summary = "Editar la cabecera y las observaciones generales (solo BORRADOR)",
            description = "Solo el guía que creó la evaluación. 409 si ya está FINALIZADA.")
    public EvaluacionDto actualizar(@PathVariable Long id, @Valid @RequestBody EvaluacionActualizarRequest request,
                                    @AuthenticationPrincipal UsuarioAutenticado actor) {
        return evaluaciones.actualizar(id, request, actor);
    }

    @PutMapping("/{id}/respuestas")
    @PreAuthorize("hasRole('PROFESOR_GUIA')")
    @Operation(summary = "Registrar los puntajes de uno o varios indicadores",
            description = "Solo el guía que creó la evaluación. Cada puntaje debe ser un entero dentro de la escala de la "
                    + "pauta (1 a 7) y el indicador debe ser de la pauta (400). Un indicador ya puntuado se reemplaza. "
                    + "409 si la evaluación está FINALIZADA.")
    public EvaluacionDto registrarRespuestas(@PathVariable Long id, @Valid @RequestBody RespuestasRequest request,
                                             @AuthenticationPrincipal UsuarioAutenticado actor) {
        return evaluaciones.registrarRespuestas(id, request, actor);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('PROFESOR_GUIA')")
    @Operation(summary = "Finalizar la evaluación",
            description = "Exige los 20 indicadores puntuados (400 si faltan), calcula el puntaje total y el promedio, "
                    + "y pasa a FINALIZADA: desde ahí no se puede modificar (409).")
    public EvaluacionDto finalizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return evaluaciones.finalizar(id, actor);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una evaluación con el puntaje de cada indicador",
            description = "Coordinador, el guía evaluador, y (solo si está FINALIZADA) el estudiante evaluado y su profesor del curso. "
                    + "Cualquier otro: 403.")
    public EvaluacionDto obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return evaluaciones.obtener(id, actor);
    }

    @GetMapping
    @Operation(summary = "Listar evaluaciones (según mi rol)",
            description = "Filtros opcionales por inscripción y estado. Cada rol ve solo lo suyo; los borradores solo los ve "
                    + "su autor y el coordinador.")
    public List<EvaluacionResumenDto> listar(@RequestParam(required = false) Long inscripcionId,
                                             @RequestParam(required = false) EstadoEvaluacion estado,
                                             @AuthenticationPrincipal UsuarioAutenticado actor) {
        return evaluaciones.listar(inscripcionId, estado, actor);
    }
}
