package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.EquipoCrearRequest;
import cl.ubb.pem.practicas.dto.EquipoDto;
import cl.ubb.pem.practicas.service.EquipoService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipos")
@Tag(name = "HU-10 Prácticas en dupla y en grupo")
public class EquipoController {

    private final EquipoService equipos;

    public EquipoController(EquipoService equipos) {
        this.equipos = equipos;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINADOR','JEFE_CARRERA')")
    @Operation(summary = "Listar duplas y grupos",
            description = "Solo COORDINADOR y JEFE_CARRERA, con filtros opcionales. Cada integrante ve su equipo en su "
                    + "inscripción (`equipo`) y en su historial.")
    public List<EquipoDto> listar(@RequestParam(required = false) Integer asignaturaId,
                                  @RequestParam(required = false) String periodo) {
        return equipos.listar(asignaturaId, periodo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR','JEFE_CARRERA')")
    @Operation(summary = "Ver un equipo")
    public EquipoDto obtener(@PathVariable Long id) {
        return equipos.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Armar una dupla o un grupo",
            description = "Solo COORDINADOR. Inscripciones de la misma asignatura y periodo, EN_CURSO y sin equipo "
                    + "(409 si no), con el mismo profesor guía. DUPLA = 2 integrantes, GRUPO = 2 a 4; una asignatura "
                    + "INDIVIDUAL no forma equipos (400). Si la asignatura va a un colegio, `establecimiento` y `curso` "
                    + "son obligatorios.")
    public EquipoDto crear(@Valid @RequestBody EquipoCrearRequest r) {
        return equipos.crear(r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Deshacer un equipo",
            description = "Solo COORDINADOR. Cada integrante vuelve a ser individual. 409 si alguna inscripción ya "
                    + "está cerrada (el equipo queda como registro histórico).")
    public void disolver(@PathVariable Long id) {
        equipos.disolver(id);
    }
}
