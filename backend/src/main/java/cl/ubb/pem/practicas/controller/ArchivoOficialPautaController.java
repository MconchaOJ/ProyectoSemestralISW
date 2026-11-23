package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.ArchivoOficialDto;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ArchivoDescarga;
import cl.ubb.pem.practicas.service.ArchivoOficialPautaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.core.io.Resource;
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

/** HU-25: archivo oficial (PDF o Word firmado y timbrado) de cada pauta, con su historial. */
@RestController
@RequestMapping("/api/pautas/{pautaId}/archivo-oficial")
@Tag(name = "HU-25 Archivos oficiales de las pautas")
public class ArchivoOficialPautaController {

    private final ArchivoOficialPautaService archivos;

    public ArchivoOficialPautaController(ArchivoOficialPautaService archivos) {
        this.archivos = archivos;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Subir o reemplazar el archivo oficial de la pauta",
            description = "Solo COORDINADOR. PDF o Word (.docx), con el tamaño máximo vigente (S-95). Si la pauta ya "
                    + "tenía uno, el nuevo pasa a ser el vigente (versión siguiente) y el anterior queda en el historial. "
                    + "Es de esta versión de la pauta: una versión nueva no lo hereda (S-96).")
    public ArchivoOficialDto subir(@PathVariable Integer pautaId, @RequestParam("archivo") MultipartFile archivo,
                                   @AuthenticationPrincipal UsuarioAutenticado actor) {
        return archivos.subir(pautaId, archivo, actor);
    }

    @GetMapping
    @Operation(summary = "Datos del archivo oficial vigente",
            description = "Cualquier rol autenticado (quien ve la pauta). 404 si la pauta no tiene archivo oficial.")
    public ArchivoOficialDto vigente(@PathVariable Integer pautaId) {
        return archivos.vigente(pautaId).orElseThrow(
                () -> new RecursoNoEncontradoException("La pauta " + pautaId + " todavía no tiene archivo oficial."));
    }

    @GetMapping("/descargar")
    @Operation(summary = "Descargar el archivo oficial vigente", description = "Cualquier rol autenticado (quien ve la pauta).")
    public ResponseEntity<Resource> descargarVigente(@PathVariable Integer pautaId) {
        return archivo(archivos.descargarVigente(pautaId));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('COORDINADOR','JEFE_CARRERA')")
    @Operation(summary = "Historial del archivo oficial",
            description = "COORDINADOR y JEFE_CARRERA. Todas las versiones del archivo, de la vigente a la primera, con quién y cuándo.")
    public List<ArchivoOficialDto> historial(@PathVariable Integer pautaId) {
        return archivos.historial(pautaId);
    }

    @GetMapping("/{archivoId}")
    @PreAuthorize("hasAnyRole('COORDINADOR','JEFE_CARRERA')")
    @Operation(summary = "Descargar una versión del historial", description = "COORDINADOR y JEFE_CARRERA.")
    public ResponseEntity<Resource> descargar(@PathVariable Integer pautaId, @PathVariable Long archivoId) {
        return archivo(archivos.descargar(pautaId, archivoId));
    }

    private static ResponseEntity<Resource> archivo(ArchivoDescarga a) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nombreArchivo()).build().toString())
                .body(a.recurso());
    }
}
