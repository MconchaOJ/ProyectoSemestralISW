package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.HistorialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
                    + "curso asignados, el coordinador y el jefe de carrera; cualquier otro recibe 403.")
    public HistorialDto historialDe(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return historial.historialDe(id, actor);
    }

    // HU-28. Sin "produces = application/pdf" a propósito, como la cartola: los errores (403, 404) salen en JSON.
    @GetMapping("/me/historial/pdf")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Mi historial en PDF (HU-28)",
            description = "Solo ESTUDIANTE: su propio historial como PDF descargable.",
            responses = @ApiResponse(responseCode = "200", description = "PDF descargable",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)))
    public ResponseEntity<byte[]> miHistorialPdf(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return pdf(historial.pdfPropio(actor), "mi-historial.pdf");
    }

    @GetMapping("/{id}/historial/pdf")
    @Operation(summary = "Historial de un estudiante en PDF (HU-28)",
            description = "Las mismas inscripciones, evaluaciones finalizadas y planificaciones que GET "
                    + "/api/estudiantes/{id}/historial, con una síntesis por inscripción y la fecha de generación. "
                    + "Mismas reglas de acceso que ver el historial: el propio estudiante, su guía, profesor del curso o "
                    + "tutor asignados, el coordinador y el jefe de carrera; cualquier otro recibe 403.",
            responses = @ApiResponse(responseCode = "200", description = "PDF descargable",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)))
    public ResponseEntity<byte[]> historialPdf(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return pdf(historial.pdfDe(id, actor), "historial-estudiante-" + id + ".pdf");
    }

    private static ResponseEntity<byte[]> pdf(byte[] contenido, String nombre) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }
}
