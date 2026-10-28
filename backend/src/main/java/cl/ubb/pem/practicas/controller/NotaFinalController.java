package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.EstructuraEvaluativaDto;
import cl.ubb.pem.practicas.dto.EstructuraEvaluativaRequest;
import cl.ubb.pem.practicas.dto.NotaComponenteRequest;
import cl.ubb.pem.practicas.dto.NotaFinalDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.NotaFinalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "HU-09 Nota final ponderada")
public class NotaFinalController {

    private final NotaFinalService notaFinal;

    public NotaFinalController(NotaFinalService notaFinal) {
        this.notaFinal = notaFinal;
    }

    @GetMapping("/api/asignaturas/{id}/componentes-nota")
    @Operation(summary = "Estructura evaluativa de una asignatura",
            description = "Componentes de la nota final con su peso (%), en orden. `completa` = hay componentes y "
                    + "suman 100. Cualquier usuario con sesión (es un catálogo, como las asignaturas).")
    public EstructuraEvaluativaDto estructura(@PathVariable Integer id) {
        return notaFinal.estructura(id);
    }

    @PutMapping("/api/asignaturas/{id}/componentes-nota")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Configurar la estructura evaluativa (criterio 1)",
            description = "Solo COORDINADOR. Reemplaza la lista completa: con `id` se edita, sin `id` se crea y lo que "
                    + "no viene se borra. Los pesos deben sumar 100 (400 si no; una lista vacía quita la estructura). "
                    + "409 si se quita un componente que ya tiene notas. Recalcula la nota de las inscripciones EN_CURSO.")
    public EstructuraEvaluativaDto guardarEstructura(@PathVariable Integer id,
                                                     @Valid @RequestBody EstructuraEvaluativaRequest r) {
        return notaFinal.guardarEstructura(id, r);
    }

    @GetMapping("/api/inscripciones/{id}/nota-final")
    @Operation(summary = "Nota final ponderada de una inscripción",
            description = "Cada componente con su nota y su aporte, la nota calculada (criterio 2; nula si falta "
                    + "alguno) y la nota de cierre. Quien puede ver la inscripción puede verla.")
    public NotaFinalDto notaFinal(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notaFinal.notaFinal(id, actor);
    }

    @PutMapping("/api/inscripciones/{id}/nota-final/componentes/{componenteId}")
    @PreAuthorize("hasRole('PROFESOR_CURSO')")
    @Operation(summary = "Registrar o corregir la nota de un componente",
            description = "Solo el PROFESOR_CURSO de esa inscripción (403 para otro), con la inscripción EN_CURSO "
                    + "(409). Nota de 1,0 a 7,0 con un decimal. Recalcula y guarda la nota de la inscripción.")
    public NotaFinalDto registrarNota(@PathVariable Long id, @PathVariable Integer componenteId,
                                      @Valid @RequestBody NotaComponenteRequest r,
                                      @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notaFinal.registrarNota(id, componenteId, r.nota(), actor);
    }
}
