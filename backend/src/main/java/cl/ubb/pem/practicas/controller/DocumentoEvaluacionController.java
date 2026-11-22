package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.DocumentoDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ArchivoDescarga;
import cl.ubb.pem.practicas.service.DocumentoEvaluacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/evaluaciones/{evaluacionId}/documentos")
@Tag(name = "Documentos adjuntos")
public class DocumentoEvaluacionController {

    private final DocumentoEvaluacionService documentos;

    public DocumentoEvaluacionController(DocumentoEvaluacionService documentos) {
        this.documentos = documentos;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROFESOR_GUIA', 'TUTOR')")
    @Operation(summary = "Subir la pauta o evaluación formal a la evaluación",
            description = "Solo el evaluador (guía o tutor). Word (.docx), Excel (.xlsx) o PDF, máximo 10 MB. "
                    + "Se puede subir al finalizar o después de finalizar la evaluación.")
    public DocumentoDto subir(@PathVariable Long evaluacionId, @RequestParam("archivo") MultipartFile archivo,
                              @AuthenticationPrincipal UsuarioAutenticado actor) {
        return documentos.subir(evaluacionId, archivo, actor);
    }

    @PutMapping(value = "/{docId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PROFESOR_GUIA', 'TUTOR')")
    @Operation(summary = "Reemplazar un documento de la evaluación (HU-22)",
            description = "Solo el evaluador, con la evaluación en borrador o finalizada (S-82) y la inscripción EN_CURSO "
                    + "(409 si está cerrada). El documento conserva su id; el archivo anterior se borra después del commit.")
    public DocumentoDto reemplazar(@PathVariable Long evaluacionId, @PathVariable Long docId,
                                   @RequestParam("archivo") MultipartFile archivo,
                                   @AuthenticationPrincipal UsuarioAutenticado actor) {
        return documentos.reemplazar(evaluacionId, docId, archivo, actor);
    }

    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('PROFESOR_GUIA', 'TUTOR')")
    @Operation(summary = "Borrar un documento de la evaluación (HU-22)",
            description = "Solo el evaluador, con la evaluación en borrador o finalizada (S-82) y la inscripción EN_CURSO "
                    + "(409 si está cerrada). El archivo se borra del disco después del commit.")
    public void borrar(@PathVariable Long evaluacionId, @PathVariable Long docId,
                       @AuthenticationPrincipal UsuarioAutenticado actor) {
        documentos.borrar(evaluacionId, docId, actor);
    }

    @GetMapping
    @Operation(summary = "Listar los documentos de la evaluación",
            description = "Quien ya puede ver la evaluación (evaluador, y si está FINALIZADA también el estudiante, "
                    + "su profesor del curso y el coordinador).")
    public List<DocumentoDto> listar(@PathVariable Long evaluacionId, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return documentos.listar(evaluacionId, actor);
    }

    @GetMapping("/{docId}")
    @Operation(summary = "Descargar un documento de la evaluación",
            description = "Quien ya puede ver la evaluación.")
    public ResponseEntity<org.springframework.core.io.Resource> descargar(@PathVariable Long evaluacionId,
            @PathVariable Long docId, @AuthenticationPrincipal UsuarioAutenticado actor) {
        ArchivoDescarga a = documentos.descargar(evaluacionId, docId, actor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nombreArchivo()).build().toString())
                .body(a.recurso());
    }
}
