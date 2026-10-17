package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.RolDto;
import cl.ubb.pem.practicas.repository.RolRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "HU-01 Autenticación")
public class RolController {

    private final RolRepository roles;

    public RolController(RolRepository roles) {
        this.roles = roles;
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Listar los 5 roles del sistema",
            description = "Solo COORDINADOR. Sirve también para demostrar el 403 con cualquier otro rol.")
    public List<RolDto> listar() {
        return roles.findAllByOrderByIdAsc().stream().map(RolDto::desde).toList();
    }
}
