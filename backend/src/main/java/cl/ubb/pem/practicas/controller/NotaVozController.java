package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.NotaVozCorregirRequest;
import cl.ubb.pem.practicas.dto.NotaVozDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ArchivoDescarga;
import cl.ubb.pem.practicas.service.NotaVozService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * HU-11 y HU-12: nota de voz del profesor evaluador con transcripción a texto. SUPUESTO S-40 (ver PROGRESO.md):
 * la transcripción inicial la genera el navegador (Web Speech API) y llega ya hecha en el POST; el backend solo
 * la guarda y permite corregirla.
 */
@RestController
@RequestMapping("/api/evaluaciones/{evaluacionId}/nota-voz")
@Tag(name = "Nota de voz")
public class NotaVozController {

    private final NotaVozService notasVoz;

    public NotaVozController(NotaVozService notasVoz) {
        this.notasVoz = notasVoz;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESOR_GUIA')")
    @Operation(summary = "Grabar la nota de voz de la evaluación",
            description = "Solo el profesor guía evaluador (403 para cualquier otro). Audio más el texto ya "
                    + "transcrito por el navegador. Como máximo una por evaluación: si ya existe, 409 (usa PUT). "
                    + "Se puede grabar con la evaluación en BORRADOR o ya FINALIZADA (SUPUESTO S-42).")
    public NotaVozDto crear(@PathVariable Long evaluacionId, @RequestParam("audio") MultipartFile audio,
                            @RequestParam("transcripcion") String transcripcion,
                            @RequestParam("duracionSegundos") Integer duracionSegundos,
                            @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notasVoz.crear(evaluacionId, audio, transcripcion, duracionSegundos, actor);
    }

    @PutMapping
    @PreAuthorize("hasRole('PROFESOR_GUIA')")
    @Operation(summary = "Corregir el texto transcrito",
            description = "Solo el profesor guía evaluador. Marca transcripcionCorregida = true.")
    public NotaVozDto corregir(@PathVariable Long evaluacionId, @Valid @RequestBody NotaVozCorregirRequest request,
                               @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notasVoz.corregirTranscripcion(evaluacionId, request.transcripcion(), actor);
    }

    @GetMapping
    @Operation(summary = "Consultar la nota de voz (duración, transcripción y enlace al audio)",
            description = "El evaluador, el estudiante evaluado, su profesor del curso y el coordinador (mismas "
                    + "reglas de acceso que ver la evaluación). 404 si todavía no hay nota de voz.")
    public NotaVozDto obtener(@PathVariable Long evaluacionId, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return notasVoz.obtener(evaluacionId, actor);
    }

    @GetMapping("/audio")
    @Operation(summary = "Descargar o reproducir el audio de la nota de voz",
            description = "Mismas reglas de acceso que consultar la nota de voz.")
    public ResponseEntity<org.springframework.core.io.Resource> audio(@PathVariable Long evaluacionId,
            @AuthenticationPrincipal UsuarioAutenticado actor) {
        ArchivoDescarga a = notasVoz.descargarAudio(evaluacionId, actor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(a.nombreArchivo()).build().toString())
                .body(a.recurso());
    }
}
