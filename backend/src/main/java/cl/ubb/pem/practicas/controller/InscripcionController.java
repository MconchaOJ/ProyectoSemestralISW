package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.InscripcionCierreRequest;
import cl.ubb.pem.practicas.dto.InscripcionCrearRequest;
import cl.ubb.pem.practicas.dto.InscripcionDto;
import cl.ubb.pem.practicas.dto.InscripcionTutorRequest;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.InscripcionService;
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
@RequestMapping("/api/inscripciones")
@Tag(name = "HU-02 Estudiantes e inscripciones")
public class InscripcionController {

    private final InscripcionService inscripciones;

    public InscripcionController(InscripcionService inscripciones) {
        this.inscripciones = inscripciones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Inscribir a un estudiante en una asignatura y periodo",
            description = "Solo COORDINADOR. Queda EN_CURSO. 409 si ya está inscrito en esa asignatura y periodo. "
                    + "400 si el guía no tiene rol PROFESOR_GUIA, si el profesor del curso no tiene rol PROFESOR_CURSO, "
                    + "o si falta el guía en una asignatura que se hace en un colegio (el ciclo inicial puede ir sin guía). "
                    + "El tutor (HU-07) es opcional y solo se acepta en Práctica Profesional, con rol TUTOR.")
    public InscripcionDto crear(@Valid @RequestBody InscripcionCrearRequest request) {
        return inscripciones.crear(request);
    }

    @GetMapping
    @Operation(summary = "Listar inscripciones (según mi rol)",
            description = "Coordinador y jefe de carrera: todas. Estudiante: las suyas. Profesor guía o del curso: las que tiene asignadas. "
                    + "Con dos roles (cuentas especiales), la suma de ambos. Filtros opcionales por estudiante, asignatura y periodo.")
    public List<InscripcionDto> listar(@RequestParam(required = false) Long estudianteId,
                                       @RequestParam(required = false) Integer asignaturaId,
                                       @RequestParam(required = false) String periodo,
                                       @AuthenticationPrincipal UsuarioAutenticado actor) {
        return inscripciones.listar(estudianteId, asignaturaId, periodo, actor);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una inscripción", description = "403 si la inscripción no es del usuario (ni está asignada a él).")
    public InscripcionDto obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return inscripciones.obtener(id, actor);
    }

    @PutMapping("/{id}/cierre")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Cerrar una inscripción con su nota final",
            description = "Solo COORDINADOR. Pasa de EN_CURSO a APROBADA o REPROBADA con su nota (1,0 a 7,0). "
                    + "Así se cargan las asignaturas aprobadas del estudiante. 409 si ya estaba cerrada.")
    public InscripcionDto cerrar(@PathVariable Long id, @Valid @RequestBody InscripcionCierreRequest request) {
        return inscripciones.cerrar(id, request);
    }

    @PutMapping("/{id}/tutor")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Asignar, cambiar o quitar el tutor de una inscripción (HU-07)",
            description = "Solo COORDINADOR. Solo en asignaturas con tutor (Práctica Profesional) y con la inscripción EN_CURSO. "
                    + "El tutor debe tener rol TUTOR, estar activo y no ser el guía ni el profesor del curso (400). "
                    + "tutorId vacío quita el tutor. Las evaluaciones que ya hizo un tutor anterior quedan a su nombre.")
    public InscripcionDto asignarTutor(@PathVariable Long id, @RequestBody InscripcionTutorRequest request) {
        return inscripciones.asignarTutor(id, request.tutorId());
    }
}
