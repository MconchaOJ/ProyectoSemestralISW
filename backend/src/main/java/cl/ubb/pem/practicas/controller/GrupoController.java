package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.GrupoDto;
import cl.ubb.pem.practicas.dto.GrupoRequest;
import cl.ubb.pem.practicas.service.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grupos")
@Tag(name = "HU-10 Duplas y grupos")
public class GrupoController {

    private final GrupoService grupos;

    public GrupoController(GrupoService grupos) {
        this.grupos = grupos;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINADOR', 'JEFE_CARRERA')")
    @Operation(summary = "Listar duplas y grupos",
            description = "COORDINADOR y JEFE_CARRERA. Filtros opcionales por asignatura y periodo. Los demás roles ven a los "
                    + "compañeros de cada inscripción en GET /api/inscripciones (campo companeros).")
    public List<GrupoDto> listar(@RequestParam(required = false) Integer asignaturaId,
                                 @RequestParam(required = false) String periodo) {
        return grupos.listar(asignaturaId, periodo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'JEFE_CARRERA')")
    @Operation(summary = "Detalle de una dupla o grupo", description = "COORDINADOR y JEFE_CARRERA. 404 si no existe.")
    public GrupoDto obtener(@PathVariable Long id) {
        return grupos.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Formar una dupla o grupo (HU-10)",
            description = "Solo COORDINADOR. Las inscripciones deben ser de la misma asignatura y periodo, de una práctica en "
                    + "DUPLA (exactamente 2) o GRUPO (2 a 4), con el mismo profesor guía (400 si no); estar EN_CURSO y no "
                    + "estar en otro grupo (409). Establecimiento y curso solo en prácticas que van a un colegio (400). "
                    + "Cada integrante conserva su inscripción, su pauta y sus planificaciones (S-77).")
    public GrupoDto crear(@Valid @RequestBody GrupoRequest request) {
        return grupos.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Cambiar los integrantes, el establecimiento o el curso de un grupo",
            description = "Solo COORDINADOR. Mismas reglas que al crear; los que salen quedan sin grupo.")
    public GrupoDto actualizar(@PathVariable Long id, @Valid @RequestBody GrupoRequest request) {
        return grupos.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Deshacer una dupla o grupo",
            description = "Solo COORDINADOR. Los integrantes quedan sin grupo; sus inscripciones, evaluaciones y "
                    + "planificaciones no cambian. 409 si algún integrante ya cerró la práctica (queda como historia).")
    public void eliminar(@PathVariable Long id) {
        grupos.eliminar(id);
    }
}
