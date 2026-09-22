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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    @PreAuthorize("hasRole('PROFESOR_GUIA')")
    @Operation(summary = "Subir la pauta o evaluación formal a la evaluación",
            description = "Solo el profesor guía evaluador. Word (.docx), Excel (.xlsx) o PDF, máximo 10 MB. "
                    + "Se puede subir al finalizar o después de finalizar la evaluación.")
    public DocumentoDto subir(@PathVariable Long evaluacionId, @RequestParam("archivo") MultipartFile archivo,
                              @AuthenticationPrincipal UsuarioAutenticado actor) {
        return documentos.subir(evaluacionId, archivo, actor);
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
