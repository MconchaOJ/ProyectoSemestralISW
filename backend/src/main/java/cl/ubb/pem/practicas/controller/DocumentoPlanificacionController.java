package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.DocumentoDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ArchivoDescarga;
import cl.ubb.pem.practicas.service.DocumentoPlanificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/planificaciones/{planificacionId}/documentos")
@Tag(name = "Documentos adjuntos")
public class DocumentoPlanificacionController {

    private final DocumentoPlanificacionService documentos;

    public DocumentoPlanificacionController(DocumentoPlanificacionService documentos) {
        this.documentos = documentos;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subir un documento a la planificación",
            description = "Solo el estudiante dueño. Word (.docx) o PDF, máximo 10 MB (400 si no cumple).")
    public DocumentoDto subir(@PathVariable Long planificacionId, @RequestParam("archivo") MultipartFile archivo,
                              @AuthenticationPrincipal UsuarioAutenticado actor) {
        return documentos.subir(planificacionId, archivo, actor);
    }

    @GetMapping
    @Operation(summary = "Listar los documentos de la planificación",
            description = "Quien ya puede ver la planificación (dueño, guía, profesor del curso, coordinador).")
    public List<DocumentoDto> listar(@PathVariable Long planificacionId, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return documentos.listar(planificacionId, actor);
    }

    @GetMapping("/{docId}")
    @Operation(summary = "Descargar un documento de la planificación",
            description = "Quien ya puede ver la planificación (dueño, guía, profesor del curso, coordinador).")
    public ResponseEntity<org.springframework.core.io.Resource> descargar(@PathVariable Long planificacionId,
            @PathVariable Long docId, @AuthenticationPrincipal UsuarioAutenticado actor) {
        ArchivoDescarga a = documentos.descargar(planificacionId, docId, actor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nombreArchivo()).build().toString())
                .body(a.recurso());
    }
}
