package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.DocumentoBaseDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.BaseConocimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** HU-29: el coordinador carga la base de conocimiento del chatbot (SUPUESTOS S-69 y S-70). */
@RestController
@RequestMapping("/api/chatbot/base-conocimiento")
@Tag(name = "Chatbot de retroalimentación")
@PreAuthorize("hasRole('COORDINADOR')")
public class BaseConocimientoController {

    private final BaseConocimientoService base;

    public BaseConocimientoController(BaseConocimientoService base) {
        this.base = base;
    }

    @GetMapping
    @Operation(summary = "Ver la base de conocimiento del chatbot",
            description = "Solo el coordinador (403 para los demás). Los 10 tipos de documento de la propuesta "
                    + "RetroPlani Mate, con el archivo vigente de cada uno (quién y cuándo lo subió) o sin cargar.")
    public List<DocumentoBaseDto> listar() {
        return base.listar();
    }

    @PostMapping(value = "/{tipo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subir o reemplazar un documento de la base de conocimiento",
            description = "Solo el coordinador. Word (.docx) o PDF, máximo 10 MB y con texto legible (400 si no). Si "
                    + "ya había uno de ese tipo, lo reemplaza: el chatbot usa el nuevo desde el siguiente mensaje.")
    public DocumentoBaseDto subir(@PathVariable String tipo, @RequestParam("archivo") MultipartFile archivo,
                                  @AuthenticationPrincipal UsuarioAutenticado actor) {
        return base.subir(tipo, archivo, actor);
    }

    @DeleteMapping("/{tipo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar un documento de la base de conocimiento",
            description = "Solo el coordinador. El chatbot deja de usarlo desde el siguiente mensaje (404 si no hay).")
    public void eliminar(@PathVariable String tipo) {
        base.eliminar(tipo);
    }
}
