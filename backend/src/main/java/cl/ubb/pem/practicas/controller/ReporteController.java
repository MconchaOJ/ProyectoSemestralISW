package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.service.CartolaPracticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "HU-31 Cartola de prácticas")
public class ReporteController {

    private final CartolaPracticasService cartola;

    public ReporteController(CartolaPracticasService cartola) {
        this.cartola = cartola;
    }

    // Sin "produces = application/pdf" a propósito: así los errores (400, 403) siguen saliendo en JSON.
    @GetMapping("/cartola-practicas")
    @PreAuthorize("hasRole('JEFE_CARRERA')")
    @Operation(summary = "Cartola de prácticas en PDF",
            description = "Solo JEFE_CARRERA. Una fila por inscripción cuya fecha de cierre cae entre `desde` y `hasta` "
                    + "(ambas incluidas): estudiante y RUT, asignatura, periodo, profesor guía, profesor del curso, estado, "
                    + "fecha y nota de cierre, cantidad de evaluaciones finalizadas y su promedio. Las inscripciones EN_CURSO "
                    + "no tienen fecha de cierre y no aparecen. 400 si `desde` es posterior a `hasta`. Solo lectura.",
            responses = @ApiResponse(responseCode = "200", description = "PDF descargable",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)))
    public ResponseEntity<byte[]> cartolaPracticas(
            @Parameter(description = "Fecha inicial (AAAA-MM-DD)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final (AAAA-MM-DD)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] pdf = cartola.pdf(desde, hasta);
        String nombre = "cartola-practicas_" + desde + "_" + hasta + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(pdf);
    }
}
