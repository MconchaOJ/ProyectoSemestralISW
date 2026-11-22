package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.EstructuraEvaluativaDto;
import cl.ubb.pem.practicas.dto.EstructuraGuardarRequest;
import cl.ubb.pem.practicas.dto.NotaExternaRequest;
import cl.ubb.pem.practicas.dto.NotaFinalDto;
import cl.ubb.pem.practicas.dto.NotasAsignaturaDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.EstructuraEvaluativaService;
import cl.ubb.pem.practicas.service.NotaFinalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "HU-09 Nota final")
public class NotaFinalController {

    private final EstructuraEvaluativaService estructura;
    private final NotaFinalService notas;

    public NotaFinalController(EstructuraEvaluativaService estructura, NotaFinalService notas) {
        this.estructura = estructura;
        this.notas = notas;
    }

    @GetMapping("/asignaturas/{asignaturaId}/estructura-evaluativa")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'JEFE_CARRERA', 'PROFESOR_CURSO')")
    @Operation(summary = "Estructura evaluativa de una asignatura",
            description = "Componentes (de PAUTA o EXTERNOS) con su porcentaje, y las pautas vigentes de la asignatura. "
                    + "`puedeEditar` = el coordinador o un profesor del curso de esa asignatura (S-83).")
    public EstructuraEvaluativaDto estructura(@PathVariable Integer asignaturaId,
                                              @AuthenticationPrincipal UsuarioAutenticado actor) {
        return estructura.obtener(asignaturaId, actor);
    }

    @PutMapping("/asignaturas/{asignaturaId}/estructura-evaluativa")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'PROFESOR_CURSO')")
    @Operation(summary = "Guardar la estructura evaluativa",
            description = "La lista completa, en orden: con `id` se actualiza, sin `id` se crea y lo que no venga se "
                    + "quita (con sus notas externas). Los porcentajes de todos suman 100 (400 si no); una pauta va en un "
                    + "solo componente y debe ser la versión vigente de una pauta de la asignatura. 403 si es un profesor "
                    + "del curso sin inscripciones en esa asignatura.")
    public EstructuraEvaluativaDto guardarEstructura(@PathVariable Integer asignaturaId,
                                                     @Valid @RequestBody EstructuraGuardarRequest r,
                                                     @AuthenticationPrincipal UsuarioAutenticado actor) {
        return estructura.guardar(asignaturaId, r, actor);
    }

    @GetMapping("/notas")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'JEFE_CARRERA', 'PROFESOR_CURSO')")
    @Operation(summary = "Notas de una asignatura en un período",
            description = "Cada inscripción que ve quien consulta (el profesor del curso, solo las suyas) con la nota de "
                    + "cada componente, la nota parcial y el porcentaje evaluado, y la nota final propuesta cuando todos "
                    + "los componentes tienen nota. 400 si el período no es AAAA-S.")
    public NotasAsignaturaDto notas(@RequestParam Integer asignaturaId, @RequestParam String periodo,
                                    @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notas.deAsignatura(asignaturaId, periodo, actor);
    }

    @GetMapping("/inscripciones/{inscripcionId}/nota-final")
    @Operation(summary = "Nota parcial o final de una inscripción",
            description = "Quien ya puede ver la inscripción (también el estudiante). Al cerrar la inscripción, la "
                    + "pantalla propone `notaPropuesta` y el coordinador la confirma.")
    public NotaFinalDto notaFinal(@PathVariable Long inscripcionId, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notas.deInscripcion(inscripcionId, actor);
    }

    @PutMapping("/inscripciones/{inscripcionId}/notas-externas/{componenteId}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'PROFESOR_CURSO')")
    @Operation(summary = "Escribir la nota de un componente externo",
            description = "El profesor del curso de esa inscripción o el coordinador (403 los demás), con la inscripción "
                    + "EN_CURSO (409 si está cerrada). Nota de 1,0 a 7,0 con un decimal. 400 si el componente es de pauta "
                    + "o de otra asignatura. Devuelve la nota recalculada.")
    public NotaFinalDto registrarNotaExterna(@PathVariable Long inscripcionId, @PathVariable Integer componenteId,
                                             @Valid @RequestBody NotaExternaRequest r,
                                             @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notas.registrarNotaExterna(inscripcionId, componenteId, r.nota(), actor);
    }

    @DeleteMapping("/inscripciones/{inscripcionId}/notas-externas/{componenteId}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'PROFESOR_CURSO')")
    @Operation(summary = "Borrar la nota de un componente externo",
            description = "Mismas reglas que escribirla. Devuelve la nota recalculada.")
    public NotaFinalDto borrarNotaExterna(@PathVariable Long inscripcionId, @PathVariable Integer componenteId,
                                          @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notas.borrarNotaExterna(inscripcionId, componenteId, actor);
    }
}
