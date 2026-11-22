package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.HistorialService;
import cl.ubb.pem.practicas.service.InformesXlsx;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final InformesXlsx xlsx;

    public HistorialController(HistorialService historial, InformesXlsx xlsx) {
        this.xlsx = xlsx;
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

    // HU-28 (y criterio 1 de HU-08): el mismo historial en PDF. Sin "produces = application/pdf" a propósito:
    // así los errores (403, 404) siguen saliendo en JSON, igual que la cartola.
    @GetMapping("/me/historial/pdf")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Mi historial en PDF", description = "Solo ESTUDIANTE: su propio historial como PDF descargable.",
            responses = @ApiResponse(responseCode = "200", description = "PDF descargable",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)))
    public ResponseEntity<byte[]> miHistorialPdf(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return Descargas.pdf(historial.pdfPropio(actor), "mi-historial-de-practicas.pdf");
    }

    @GetMapping("/{id}/historial/pdf")
    @Operation(summary = "Historial de un estudiante en PDF (HU-28; informe por estudiante de HU-08)",
            description = "Lo mismo que GET /api/estudiantes/{id}/historial, como PDF, con la fecha de generación. Mismas "
                    + "reglas de acceso: el propio estudiante, su guía, profesor del curso o tutor asignados, el coordinador "
                    + "y el jefe de carrera; cualquier otro recibe 403.",
            responses = @ApiResponse(responseCode = "200", description = "PDF descargable",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)))
    public ResponseEntity<byte[]> historialPdf(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return Descargas.pdf(historial.pdfDe(id, actor), "historial-estudiante-" + id + ".pdf");
    }

    // HU-08, informe por estudiante en Excel (integrado del trabajo de Alej): mismas reglas de acceso que el historial
    @GetMapping("/me/historial/xlsx")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Mi historial en Excel", description = "Solo ESTUDIANTE: su propio historial como libro .xlsx.")
    public ResponseEntity<byte[]> miHistorialXlsx(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return Descargas.xlsx(xlsx.historial(historial.historialPropio(actor)), "mi-historial-de-practicas.xlsx");
    }

    @GetMapping("/{id}/historial/xlsx")
    @Operation(summary = "Historial de un estudiante en Excel (informe por estudiante de HU-08)",
            description = "Lo mismo que el PDF del historial, como libro .xlsx (hojas Estudiante, Prácticas, Evaluaciones y "
                    + "Planificaciones). Mismas reglas de acceso que el historial: 403 para quien no lo puede ver, 404 si no existe.")
    public ResponseEntity<byte[]> historialXlsx(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return Descargas.xlsx(xlsx.historial(historial.historialDe(id, actor)), "historial-estudiante-" + id + ".xlsx");
    }
}
