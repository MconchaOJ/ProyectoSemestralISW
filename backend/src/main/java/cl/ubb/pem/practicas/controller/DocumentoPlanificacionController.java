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
            description = "Solo el estudiante dueño, con la planificación en BORRADOR o CON_OBSERVACIONES (409 si no). "
                    + "Word (.docx) o PDF, máximo 10 MB (400 si no cumple).")
    public DocumentoDto subir(@PathVariable Long planificacionId, @RequestParam("archivo") MultipartFile archivo,
                              @AuthenticationPrincipal UsuarioAutenticado actor) {
        return documentos.subir(planificacionId, archivo, actor);
    }

    @PutMapping(value = "/{docId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Reemplazar un documento de la planificación (HU-21)",
            description = "Mismas reglas que subir. El documento conserva su id; el archivo anterior se borra del disco "
                    + "solo si el cambio se guarda (si falla, queda el original).")
    public DocumentoDto reemplazar(@PathVariable Long planificacionId, @PathVariable Long docId,
                                   @RequestParam("archivo") MultipartFile archivo,
                                   @AuthenticationPrincipal UsuarioAutenticado actor) {
        return documentos.reemplazar(planificacionId, docId, archivo, actor);
    }

    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar un documento de la planificación (HU-21)",
            description = "Solo el estudiante dueño, en BORRADOR o CON_OBSERVACIONES (409 si no). Se puede borrar el "
                    + "último: al enviar se vuelve a exigir al menos uno (S-47). El archivo se borra después del commit.")
    public void borrar(@PathVariable Long planificacionId, @PathVariable Long docId,
                       @AuthenticationPrincipal UsuarioAutenticado actor) {
        documentos.borrar(planificacionId, docId, actor);
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
