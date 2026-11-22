package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.InformeAsignaturaDto;
import cl.ubb.pem.practicas.service.CartolaPracticasService;
import cl.ubb.pem.practicas.service.InformeAsignaturaService;
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
@Tag(name = "Informes: HU-08 por asignatura y HU-31 cartola de prácticas")
public class ReporteController {

    private final CartolaPracticasService cartola;
    private final InformeAsignaturaService informes;

    public ReporteController(CartolaPracticasService cartola, InformeAsignaturaService informes) {
        this.cartola = cartola;
        this.informes = informes;
    }

    @GetMapping("/asignatura")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'JEFE_CARRERA')")
    @Operation(summary = "Informe de una asignatura en un período (HU-08)",
            description = "Solo COORDINADOR y JEFE_CARRERA. Inscripciones por estado; notas finales (promedio, mínimo, máximo "
                    + "y tramos: menos de 4,0, 4,0 a menos de 5,0, 5,0 a menos de 6,0, 6,0 a 7,0); evaluaciones finalizadas "
                    + "por pauta (puntaje promedio, % de logro, distribución del promedio y puntaje promedio por indicador en "
                    + "cada dimensión); planificaciones por estado y cuántas fueron devueltas; una fila por estudiante. "
                    + "400 si el período no tiene el formato AAAA-S; 404 si la asignatura no existe. Solo lectura.")
    public InformeAsignaturaDto informeAsignatura(
            @Parameter(description = "Id de la asignatura de práctica (1 a 6)", example = "3") @RequestParam Integer asignaturaId,
            @Parameter(description = "Período AAAA-S", example = "2024-2") @RequestParam String periodo) {
        return informes.informe(asignaturaId, periodo);
    }

    @GetMapping("/asignatura/pdf")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'JEFE_CARRERA')")
    @Operation(summary = "Informe de una asignatura en un período, en PDF (HU-08)",
            description = "Lo mismo que GET /api/reportes/asignatura, como PDF descargable. Mismos roles y errores.",
            responses = @ApiResponse(responseCode = "200", description = "PDF descargable",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)))
    public ResponseEntity<byte[]> informeAsignaturaPdf(@RequestParam Integer asignaturaId, @RequestParam String periodo) {
        return Descargas.pdf(informes.pdf(asignaturaId, periodo), "informe-asignatura-" + asignaturaId + "_" + periodo + ".pdf");
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
