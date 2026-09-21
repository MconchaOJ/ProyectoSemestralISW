package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.ProfesorCrearRequest;
import cl.ubb.pem.practicas.dto.RolProfesor;
import cl.ubb.pem.practicas.dto.UsuarioDto;
import cl.ubb.pem.practicas.service.ProfesorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesores")
@Tag(name = "HU-02 Estudiantes e inscripciones")
public class ProfesorController {

    private final ProfesorService profesores;

    public ProfesorController(ProfesorService profesores) {
        this.profesores = profesores;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Registrar un profesor",
            description = "Solo COORDINADOR. El rol debe ser PROFESOR_GUIA o PROFESOR_CURSO. Correo único (409).")
    public UsuarioDto crear(@Valid @RequestBody ProfesorCrearRequest request) {
        return profesores.crear(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Listar profesores", description = "Solo COORDINADOR. Filtro opcional por rol. Sirve para obtener los ids que pide la inscripción.")
    public List<UsuarioDto> listar(@RequestParam(required = false) RolProfesor rol) {
        return profesores.listar(rol);
    }
}
