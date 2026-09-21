package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.EstudianteCrearRequest;
import cl.ubb.pem.practicas.dto.EstudianteDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.EstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estudiantes")
@Tag(name = "HU-02 Estudiantes e inscripciones")
public class EstudianteController {

    private final EstudianteService estudiantes;

    public EstudianteController(EstudianteService estudiantes) {
        this.estudiantes = estudiantes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Registrar un estudiante",
            description = "Solo COORDINADOR. Crea el usuario (rol ESTUDIANTE, clave guardada con BCrypt) y su ficha. "
                    + "Correo y RUT únicos (409). El RUT se valida con su dígito verificador (400).")
    public EstudianteDto crear(@Valid @RequestBody EstudianteCrearRequest request) {
        return estudiantes.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Listar y buscar estudiantes",
            description = "Solo COORDINADOR. `buscar` filtra por nombre, apellidos o RUT (sin distinguir mayúsculas ni tildes).")
    public List<EstudianteDto> listar(@RequestParam(required = false) String buscar) {
        return estudiantes.listar(buscar);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Mi ficha de estudiante", description = "Solo ESTUDIANTE: devuelve la ficha del usuario del token.")
    public EstudianteDto miFicha(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return estudiantes.obtenerPorUsuario(actor.id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Ficha de un estudiante", description = "Solo COORDINADOR.")
    public EstudianteDto obtener(@PathVariable Long id) {
        return estudiantes.obtener(id);
    }
}
