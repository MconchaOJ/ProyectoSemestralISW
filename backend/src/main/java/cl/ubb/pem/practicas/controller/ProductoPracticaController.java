package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.ProductoDto;
import cl.ubb.pem.practicas.entity.TipoProducto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ArchivoDescarga;
import cl.ubb.pem.practicas.service.ProductoPracticaService;
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

/** HU-18: informes y productos de la práctica, por inscripción. */
@RestController
@RequestMapping("/api/inscripciones/{inscripcionId}/productos")
@Tag(name = "Informes y productos de la práctica")
public class ProductoPracticaController {

    private final ProductoPracticaService productos;

    public ProductoPracticaController(ProductoPracticaService productos) {
        this.productos = productos;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subir un informe o producto de la práctica (HU-18)",
            description = "Solo el estudiante de la inscripción y con la inscripción EN_CURSO (403 / 409, S-93). "
                    + "tipo: INFORME_FINAL, DIARIO_REFLEXION, PROYECTO_INTERVENCION u OTRO. Word (.docx) o PDF, "
                    + "con el tamaño máximo vigente (400 si no cumple).")
    public ProductoDto subir(@PathVariable Long inscripcionId, @RequestParam TipoProducto tipo,
                             @RequestParam("archivo") MultipartFile archivo,
                             @AuthenticationPrincipal UsuarioAutenticado actor) {
        return productos.subir(inscripcionId, tipo, archivo, actor);
    }

    @PutMapping(value = "/{productoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Reemplazar el archivo de un producto (HU-18)",
            description = "Mismas reglas que subir. Conserva el id; tipo es opcional (si falta, se mantiene). "
                    + "El archivo anterior se borra del disco solo si el cambio se guarda.")
    public ProductoDto reemplazar(@PathVariable Long inscripcionId, @PathVariable Long productoId,
                                  @RequestParam(required = false) TipoProducto tipo,
                                  @RequestParam("archivo") MultipartFile archivo,
                                  @AuthenticationPrincipal UsuarioAutenticado actor) {
        return productos.reemplazar(inscripcionId, productoId, tipo, archivo, actor);
    }

    @DeleteMapping("/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar un producto (HU-18)",
            description = "Solo el estudiante de la inscripción, con la inscripción EN_CURSO. El archivo se borra después del commit.")
    public void borrar(@PathVariable Long inscripcionId, @PathVariable Long productoId,
                       @AuthenticationPrincipal UsuarioAutenticado actor) {
        productos.borrar(inscripcionId, productoId, actor);
    }

    @GetMapping
    @Operation(summary = "Listar los informes y productos de la inscripción (HU-18)",
            description = "Quien ya puede ver la inscripción (estudiante, guía, profesor del curso, tutor, coordinador y jefe de carrera).")
    public List<ProductoDto> listar(@PathVariable Long inscripcionId, @AuthenticationPrincipal UsuarioAutenticado actor) {
        return productos.listar(inscripcionId, actor);
    }

    @GetMapping("/{productoId}")
    @Operation(summary = "Descargar un informe o producto (HU-18)", description = "Quien ya puede ver la inscripción.")
    public ResponseEntity<org.springframework.core.io.Resource> descargar(@PathVariable Long inscripcionId,
            @PathVariable Long productoId, @AuthenticationPrincipal UsuarioAutenticado actor) {
        ArchivoDescarga a = productos.descargar(inscripcionId, productoId, actor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nombreArchivo()).build().toString())
                .body(a.recurso());
    }
}
